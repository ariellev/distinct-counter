package org.some.thing.component;

import org.some.thing.commons.Constants;
import org.some.thing.counter.ICounter;
import org.some.thing.commons.Model;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Worker extends AbstractConsumer<ICounter> {

    private static Logger log = LogManager.getLogger(Worker.class);
    private final Class<? extends ICounter> counterClazz;
    private final ICounter counter;
    private final Source producer;
    private final String prop;
    private Long from;
    private final String downstreamTopic;

    // sliding window, seconds
    private long window = 60;
    private long windowMs = 1000;

    public Worker(String groupId, String clientId, Boolean sticky, String prop, Boolean isMetric, Class<? extends ICounter> clazz, long window) throws IllegalAccessException, InstantiationException {
        super(groupId, clientId, sticky, Constants.Topics.topic(prop));
        this.counterClazz = clazz;
        this.window = window;
        this.windowMs = 1000 * window;
        this.counter = counterClazz.newInstance();
        this.producer = new Source();
        this.prop = prop;
        this.from = null;
        this.downstreamTopic = isMetric? Constants.Topics.METRICS : Constants.Topics.CARDINALITIES;
    }

    private void sendCardinality() {

        log.debug("sending cardinality, ts={}, prop={}, topic={}", Constants.sdf.format(new Date(from)), prop, downstreamTopic);

        final Model.Cardinality card = Model.Cardinality.newBuilder()
                .setTime(from)
                .setProperty(prop)
                .setMethod(counterClazz.getSimpleName())
                .setWindow(window)
                .setCardinality(counter.cardinality())
                .setError(counter.error())
                .build();

        producer.send(downstreamTopic, prop, card.toByteArray());
    }

    @Override
    public ICounter call() throws Exception {

        this.poll((records) -> {

            for (ConsumerRecord<String, byte[]> record : records) {

                try {

                    Model.Ingess ingess = Model.Ingess.parseFrom(record.value());

                    long ts = ingess.getTime();
                    if (from == null) {
                        from = ts;
                    }

                    log.debug("prop={}, from={}, to={}, ts={}", prop, Constants.sdf.format(new Date(from)), Constants.sdf.format(new Date(from + this.windowMs)), Constants.sdf.format(new Date(ts)));

                    if (ts >= from && ts < from + this.windowMs) {
                        counter.add(ingess.getValue());
                    } else if (ts >= from + this.windowMs) {
                        this.sendCardinality();
                        counter.clear();
                        from = ts;
                    }
                } catch (Exception e) {
                    log.error("", e);
                }

            }
            return null;
        });

        return counter;
    }

    @Override
    protected void shutdown() {
        // Gracefully handling leftover data
        if (!counter.isEmpty()) {
            this.sendCardinality();
        }

        producer.close();
    }

    public static void main(String[] args) throws Exception {

        final Config conf = ConfigFactory.load().getConfig("worker");
        final String uuid = UUID.randomUUID().toString();
        final Boolean sticky = conf.getBoolean("sticky");

        final Class<? extends ICounter> clazz = (Class<? extends ICounter>) Class.forName("org.some.thing.counter." + conf.getString("method"));
        final String property = conf.getString("property");
        final Boolean isMetric = conf.getBoolean("metric");

        final String gid = String.format(conf.getString("gid"), clazz.getSimpleName().toLowerCase());

        final String cid = String.format("%s-%s", gid, uuid);

        final long window = conf.getDuration("window", TimeUnit.SECONDS);

        new Worker(gid, cid, sticky, property, isMetric, clazz, window).run();
    }

}
