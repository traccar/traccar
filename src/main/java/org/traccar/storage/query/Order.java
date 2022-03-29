package org.traccar.storage.query;

public class Order {

    private final String column;
    private final boolean descending;

    public Order(String column) {
        this(false, column);
    }

    public Order(boolean descending, String column) {
        this.column = column;
        this.descending = descending;
    }

    public String getColumn() {
        return column;
    }

    public boolean getDescending() {
        return descending;
    }

}
