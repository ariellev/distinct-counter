package org.some.thing.component;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.some.thing.commons.Constants;
import org.some.thing.commons.Model;

import java.util.Date;

public class CardinalitySink extends CSVSink {

    private static Logger log = LogManager.getLogger(CardinalitySink.class);

    private final String fmt = "%s;%s;%s;%s;%s;%s\n";

    public CardinalitySink(String groupId, String clientId, Boolean sticky, String path) {
        super(groupId, clientId, Constants.Topics.CARDINALITIES, sticky, path);
    }

    @Override
    protected String header() {
        return String.format(fmt, "date", "property", "method", "window", "cardinality", "error");
    }

    @Override
    protected String toSinkRecord(byte[] blob) throws InvalidProtocolBufferException {
        Model.Cardinality card = Model.Cardinality.parseFrom(blob);
        return String.format(fmt,
                Constants.sdf.format(new Date(card.getTime())),
                card.getProperty(),
                card.getMethod(),
                card.getWindow(),
                card.getCardinality(),
                card.getError());
    }

    public static void main(String[] args) throws Exception {

        CSVSink.factory(CardinalitySink.class).run();

    }

}
