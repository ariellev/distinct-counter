package org.some.thing.component;

import com.google.protobuf.InvalidProtocolBufferException;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.some.thing.commons.Constants;
import org.some.thing.commons.Model;

public class JsonSink extends Sink {

    private static Logger log = LogManager.getLogger(JsonSink.class);

    private final String fmt = "%s;%s;%s;%s;%s;%s\n";

    public JsonSink(String groupId, String clientId, Boolean sticky, Boolean isMetric, String path) {
        super(groupId, clientId, isMetric? Constants.Topics.METRICS : Constants.Topics.CARDINALITIES, sticky, path);
    }

    @Override
    protected String header() {
        return null;
    }

    @Override
    protected String toSinkRecord(byte[] blob) throws InvalidProtocolBufferException {
        Model.Cardinality card = Model.Cardinality.parseFrom(blob);
        return new JsonFormat().printToString(card) + "\n";
    }

    public static void main(String[] args) throws Exception {
        Sink.factory(JsonSink.class, "json").run();
    }

}
