package org.some.thing.counter;

public interface ICounter<T> {

    /**
     * Add an element to the counter
     *
     * @param element
     */
    void add(T element);

    /**
     * Add a hashed element to the counter
     *
     * @param hash
     */
    void addHash(long hash);

    /**
     * @return cardinality of counted elements
     */
    int cardinality();

    /**
     * @return count error
     */
    double error();

    /**
     * @return count bias
     */
    double bias();

    /**
     * Resets counter
     */
    void clear();

    /**
     * Has counted elements
     */
    boolean isEmpty();
}
