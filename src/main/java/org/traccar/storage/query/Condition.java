package org.traccar.storage.query;

public interface Condition {

    class Equals implements Condition {
        private final String column;
        private final String variable;
        private final Object value;

        public Equals(String column, String variable, Object value) {
            this.column = column;
            this.variable = variable;
            this.value = value;
        }

        public String getColumn() {
            return column;
        }

        public String getVariable() {
            return variable;
        }

        public Object getValue() {
            return value;
        }
    }

    class Between implements Condition {
        private final String column;
        private final String fromVariable;
        private final Object fromValue;
        private final String toVariable;
        private final Object toValue;

        public Between(String column, String fromVariable, Object fromValue, String toVariable, Object toValue) {
            this.column = column;
            this.fromVariable = fromVariable;
            this.fromValue = fromValue;
            this.toVariable = toVariable;
            this.toValue = toValue;
        }

        public String getColumn() {
            return column;
        }

        public String getFromVariable() {
            return fromVariable;
        }

        public Object getFromValue() {
            return fromValue;
        }

        public String getToVariable() {
            return toVariable;
        }

        public Object getToValue() {
            return toValue;
        }
    }

    class And implements Condition {
        private final Condition first;
        private final Condition second;

        public And(Condition first, Condition second) {
            this.first = first;
            this.second = second;
        }

        public Condition getFirst() {
            return first;
        }

        public Condition getSecond() {
            return second;
        }
    }

}
