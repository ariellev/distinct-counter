package org.some.thing.commons;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class Context {

    public Context send(String topic, String key, String... strs) {
        Stream.of(strs).forEach(s -> this.send(topic, key, s.getBytes()));
        return this;
    }

    public abstract Context send(String topic, String key, byte[] content);

    public Context streamFromPath(String topic, String key, String path, Function<String, byte[]> decoder) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)))) {
            return this.stream(topic, key, reader, decoder);
        } catch (Exception e) {
            e.printStackTrace();
            return this;
        }
    }

    public Context streamFromClasspath(String topic, String key, String resource, Function<String, byte[]> decoder) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(resource)))) {
            return this.stream(topic, key, reader, decoder);
        } catch (Exception e) {
            e.printStackTrace();
            return this;
        }
    }

    public Context stream(String topic, String key, BufferedReader resource, Function<String, byte[]> decoder) {
        Stream<byte[]> stream = resource.lines().map(decoder);
        stream.forEach(blob -> this.send(topic, key, blob));
        return this;
    }
}
