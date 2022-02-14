package org.traccar.storage.query;

public class Request {

    private final Columns columns;
    private final Condition condition;
    private final Order order;
    private final Limit limit;

    public Request(Columns columns) {
        this(columns, null, null);
    }

    public Request(Condition condition) {
        this(null, condition, null);
    }

    public Request(Columns columns, Condition condition) {
        this(columns, condition, null);
    }

    public Request(Columns columns, Condition condition, Order order) {
        this(columns, condition, order, null);
    }

    public Request(Columns columns, Condition condition, Order order, Limit limit) {
        this.columns = columns;
        this.condition = condition;
        this.order = order;
        this.limit = limit;
    }

    public Columns getColumns() {
        return columns;
    }

    public Condition getCondition() {
        return condition;
    }

    public Order getOrder() {
        return order;
    }

    public Limit getLimit() {
        return limit;
    }

}
