package org.some.thing.commons;

import java.text.SimpleDateFormat;

public class Constants {

    public static final String datePattern = "yyyy-MM-dd HH:mm:ss";
    public static final SimpleDateFormat sdf = new SimpleDateFormat(datePattern);

    public static class Metrics {
        public static final String FRAMES_PROCESSED = "frames-processed";
        public static final String FRAMES_INGESTED = "frames-ingested";
    }

    public static class Topics {

        public static String topic(String suffix) {
            return String.format("distinct-counter-%s", suffix);
        }

        public static final String INGEST = topic("ingest");
        public static final String CARDINALITIES = topic("cardinalities");
        public static final String METRICS = topic("metrics");
        public static final String TEST = topic("test");

    }
}
