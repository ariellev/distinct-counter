package org.some.thing.component;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.some.thing.commons.Constants;
import org.some.thing.commons.Model;
import org.some.thing.commons.Utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Doorman extends AbstractConsumer<Void> {

    private static Logger log = LogManager.getLogger(Doorman.class);
    private final Source producer;
    private Map<String, String> ingress;

    public Doorman(String groupId, String clientId, Boolean sticky, Map ingress) {
        super(groupId, clientId, sticky, Constants.Topics.INGEST);
        this.ingress = ingress;
        this.producer = new Source();
    }

    private int sendProperty(String prop, Object value, Long ts) {

        // fully qualified topic name
        final String fqtn = Constants.Topics.topic(prop);

        byte[] blob = Model.Ingess.newBuilder()
                .setTime(ts)
                .setValue(value.toString())
                .setProperty(prop)
                .build().toByteArray();

        producer.send(fqtn, prop, blob);
        return 1;
    }

    @Override
    public Void call() throws Exception {
        this.poll((records) -> {
            int total = 0;
            for (ConsumerRecord<String, byte[]> record : records) {
                final String json = new String(record.value());
                Long ts = Long.valueOf(Utils.jGet(json, "ts", "[0-9]+"));

                if (ts != null) {
                    total += ingress.entrySet().stream().mapToInt(entry -> {
                        final String prop = entry.getKey();
                        final String enforce = entry.getValue();

                        final String value = Utils.jGet(json, prop, enforce);
                        return (value != null) ? this.sendProperty(prop, value, ts*1000) : 0;
                    }).sum();
                }
            }

            final long now = new Date().getTime();

            this.sendProperty(Constants.Metrics.FRAMES_PROCESSED, records.count(), now);
            this.sendProperty(Constants.Metrics.FRAMES_INGESTED, total, now);

            log.trace("Ingress, total={}", total);
            return null;
        });

        return null;
    }

    @Override
    protected void shutdown() {
        producer.close();
    }

    public static void main(String[] args) throws Exception {

        // Config
        final Config conf = ConfigFactory.load().getConfig("doorman");
        final String uuid = UUID.randomUUID().toString();
        final Boolean sticky = conf.getBoolean("sticky");

        final String gid = conf.getString("gid");
        final String cid = String.format(conf.getString("cid"), uuid);

        final Config ingressConfig = conf.getConfig("ingress");

        Map<String, String> ingress = new HashMap<>();
        for (Map.Entry<String, ConfigValue> entry : ingressConfig.entrySet()) {
            ingress.put(entry.getKey(), entry.getValue().render().replace("\"", ""));
        }

        new Doorman(gid, cid, sticky, ingress).run();
    }

}
