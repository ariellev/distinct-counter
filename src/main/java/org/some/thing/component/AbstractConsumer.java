package org.some.thing.component;

import org.some.thing.commons.Utils;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public abstract class AbstractConsumer<T> implements Callable<T> {

    private static Logger log = LogManager.getLogger(AbstractConsumer.class);

    private ExecutorService executor = Executors.newFixedThreadPool(2);

    private Boolean sticky = false;
    private final String groupId;
    private final String clientId;
    private final String topic;
    private final KafkaConsumer consumer;

    public AbstractConsumer(String groupId, String clientId, Boolean sticky, String topic) {
        this.groupId = groupId;
        this.clientId = clientId;
        this.topic = topic;
        this.consumer = Utils.C.Kafka.consumer(clientId, groupId);
        this.consumer.subscribe(Arrays.asList(this.topic));
        this.sticky = sticky;
        log.info("Client id={} subscribed to topic {}", clientId, topic);
    }

    public T run() throws ExecutionException, InterruptedException {

        // setup code
        final Long start = new Date().getTime();

        // adding a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Graceful shutdown, waiting 5s");
            try {
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);
                this.shutdown();

                System.out.println(String.format("Closing consumer, cid=%s", clientId));
                consumer.close();
            } catch (Exception e) {

            } finally {
                System.out.println("Shutdown completed");
                executor.shutdownNow();
            }
        }));

        // this call should basically only return on interruption
        T res = this.executor.submit(this).get();

        final Long end = new Date().getTime();
        log.info("Finished processing data, took={}s", (end - start) / 1000);
        return res;
    }

    protected abstract void shutdown();

    protected void poll(Function<ConsumerRecords<String, byte[]>, Void> f) {

        try {

            boolean flag = true;

            while (flag) {
                ConsumerRecords<String, byte[]> records = consumer.poll(1000);
                flag = !records.isEmpty() || sticky;

                if (!records.isEmpty()) {
                    log.trace("Processing {} records", records.count());
                    f.apply(records);
                }
            }

        } catch (WakeupException e) {

        }
    }

}
