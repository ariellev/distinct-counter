package org.some.thing.component;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.UUID;

public abstract class Sink extends AbstractConsumer<Void> {

    private static Logger log = LogManager.getLogger(Sink.class);

    private BufferedWriter writer;
    private final String path;

    public Sink(String groupId, String clientId, String topic, Boolean sticky, String path) {
        super(groupId, clientId, sticky, topic);
        this.path = path;
    }

    protected abstract String header();

    protected abstract String toSinkRecord(byte[] blob) throws Exception;

    @Override
    public Void call() throws Exception {

        final Boolean exists = new File(path).exists();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, true))) {

            this.writer = writer;

            if (!exists && this.header() != null) {
                writer.write(this.header());
                writer.flush();
            }

            this.poll((records) -> {
                for (ConsumerRecord<String, byte[]> record : records) {

                    try {

                        final String sinkRecord = toSinkRecord(record.value());
                        writer.write(sinkRecord);

                    } catch (Exception e) {
                        log.error("", e);
                    }
                }

                if (!records.isEmpty()) {
                    log.debug("Flushing {} records, path={}", records.count(), path);
                    try {
                        writer.flush();
                    } catch (IOException e) {
                        log.error("", e);
                    }
                }

                return null;
            });
        } finally {


        }

        return null;
    }

    @Override
    protected void shutdown() {
        try {
            writer.flush();
        } catch (IOException e) {
            log.error("", e);
        }
    }

    protected static Sink factory(final Class<? extends Sink> clazz, final String extension) throws Exception {
        // Config
        final String uuid = UUID.randomUUID().toString();

        System.setProperty("sink.ext", extension);
        System.setProperty("sink.uuid", uuid);

        final Config conf = ConfigFactory.load().getConfig("sink");

        final Boolean sticky = conf.getBoolean("sticky");

        final String gid = conf.getString("gid");
        final String cid = conf.getString("cid");
        final String path = conf.getString("path");

        final Boolean isMetric = conf.getBoolean("metric");

        Constructor<?> constructor = clazz.getConstructor(String.class, String.class, Boolean.class, Boolean.class, String.class);
        return (Sink)constructor.newInstance(gid, cid, sticky, isMetric, path);
    }

}
