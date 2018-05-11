package org.some.thing.counter;

import java.util.HashSet;
import java.util.Set;

/**
 * Exact Counter
 * @param <T>
 */
public class Exact<T> implements ICounter<T> {

    private Set<T> counts = new HashSet<>();

    @Override
    public void add(T element) {
        counts.add(element);
    }

    @Override
    public void addHash(long hash) {

    }

    @Override
    public int cardinality() {
        return counts.size();
    }

    @Override
    public double error() {
        return 0;
    }

    @Override
    public double bias() {
        return 0;
    }

    @Override
    public void clear() {
        counts.clear();
    }

    @Override
    public boolean isEmpty() {
        return counts.isEmpty();
    }

}
