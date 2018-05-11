package org.some.thing.commons;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static Logger log = LogManager.getLogger(Utils.class);

    /**
     * Lookup a a value of a primitive json property specified by property
     * and enforced by a regular expression
     *
     * @param json     json object as string
     * @param property specified json property
     * @param enforce  regex pattern to enforce on the value
     * @return value as String
     */
    public static String jGet(String json, String property, String enforce) {
        final String regex = String.format("\"%s\":[ ]*\"?([^,}\"]+).*", property);
        final Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(json);

        String result = "";
        int i = 0;
        while (matcher.find(i) && !result.matches(enforce)) {
            result = matcher.group(1);
            i = json.indexOf(result, i) + 1;
        }

        return result.matches(enforce) ? result : null;
    }

    public static Map<String, Object> toMap(Config conf) {
        Map<String, Object> config = new HashMap<>();

        for (Map.Entry<String, ConfigValue> entry : conf.entrySet()) {
            config.put(entry.getKey(), entry.getValue().unwrapped());
        }

        return config;
    }

    /**
     * Factory methods for object creation out of typesafe Config file
     * C - for configuration
     **/
    public static class C {
        public static class Kafka {
            public static KafkaProducer producer() {
                Config kafkaConf = ConfigFactory.load().getConfig("kafka.producer");
                Map<String, Object> config = Utils.toMap(kafkaConf);
                return new KafkaProducer(config);
            }

            public static KafkaConsumer consumer() {
                return Utils.C.Kafka.consumer(null, null);
            }

            public static KafkaConsumer consumer(String clientId, String groupId) {
                Config kafkaConf = ConfigFactory.load().getConfig("kafka.consumer");
                Map<String, Object> config = Utils.toMap(kafkaConf);

                if (clientId != null) {
                    config.put("client.id", clientId);
                }

                if (groupId != null) {
                    config.put("group.id", groupId);
                }

                return new KafkaConsumer(config);
            }
        }
    }
}
