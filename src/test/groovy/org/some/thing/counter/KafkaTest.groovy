package org.some.thing.counter

import org.some.thing.commons.Constants
import org.some.thing.commons.Utils
import org.apache.kafka.common.TopicPartition
import org.some.thing.component.Source
import spock.lang.Specification

class KafkaTest extends Specification {

    def "producer streams input file"() {

        setup:
        def producer = new Source()
        def consumer = Utils.C.Kafka.consumer()
        def topic = Constants.Topics.TEST
        def partition = new TopicPartition(topic, 0)

        when:
        consumer.subscribe([topic])
        consumer.poll(100)

        def position = consumer.position(partition)

        producer.streamFromClasspath(topic, "1", "/stream.jsonl.head", {String line -> line.getBytes()})
        consumer.poll(100)

        then:
        consumer.position(partition) - position > 0

        cleanup:
        producer.close()
        consumer.close()
    }
}
