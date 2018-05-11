package org.some.thing.commons;

import org.some.thing.component.CardinalitySink;
import org.some.thing.component.Doorman;
import org.some.thing.component.Source;
import org.some.thing.component.Worker;

import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        final List<String> list = Arrays.asList(
                Doorman.class.getName(),
                Worker.class.getName(),
                CardinalitySink.class.getName(),
                Source.class.getName());

        final String joined = String.join(" | ", list);
        System.out.println("You'll need to specify a main class, java -cp JAR [" + joined + "]");
    }

}
