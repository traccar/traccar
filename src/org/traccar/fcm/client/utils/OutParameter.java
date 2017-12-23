package org.traccar.fcm.client.utils;

/**
 * Out Parameter to enable try-Methods for simpler code. A try method using an
 * OutParameter should always initialize the OutParameter first, so you have a
 * valid reference.
 *
 * @param <E> Out Result
 */
public class OutParameter<E> {

    private E ref;

    public OutParameter() {
    }

    /**
     * Gets the Result of the OutParameter.
     *
     * @return Result
     */
    public E get() {
        return ref;
    }

    /**
     * Sets the OutParameter.
     *
     * @param e Result
     */
    public void set(E e) {
        this.ref = e;
    }

    /**
     * Overrides the toString Method to print the reference
     * of the OutParameter instead of itself.
     *
     * @return String Representation of the Result.
     */
    public String toString() {
        return ref.toString();
    }
}
