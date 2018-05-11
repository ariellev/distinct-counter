package org.some.thing.counter;

/**
 * Count Head. 1 element = 1 count
 * @param <T>
 */
public class Head<T> implements ICounter<T> {

    private int count = 0;

    @Override
    public void add(T element) {
        count++;
    }

    @Override
    public void addHash(long hash) {

    }

    @Override
    public int cardinality() {
        return count;
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
        count = 0;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

}
