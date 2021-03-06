package org.some.thing.component;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.some.thing.commons.Constants;
import org.some.thing.commons.Context;
import org.some.thing.commons.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class Source extends Context {

    private static Logger log = LogManager.getLogger(Source.class);

    private final KafkaProducer producer;

    public Source() {
        this.producer = Utils.C.Kafka.producer();
    }

    public void close() {
        this.producer.close();
    }

    @Override
    public Context send(String topic, String key, byte[] content) {
        ProducerRecord<String, byte[]> record = new ProducerRecord(topic, key, content);
        producer.send(record);
        return this;
    }

    public static void main(String[] args) throws IOException {

        final Source source = new Source();
        final Config conf = ConfigFactory.load().getConfig("source");
        final String path = conf.getString("path");

        if (!new File(path).exists()) {
            log.error("File not found, path={}", path);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path))))) {
            source.stream(Constants.Topics.INGEST, null, reader, line -> {
                return line.getBytes();
            });
        } catch(Exception e) {

            log.error(e.getMessage());

        } finally {

        }

        source.close();
        log.info("Finished streaming records, topic={}", Constants.Topics.INGEST);
    }

}
