package org.traccar.storage.query;

public class Limit {

    private final int value;

    public Limit(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
