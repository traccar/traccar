package org.traccar.storage.query;

public interface Condition {

    class Equals extends Compare {
        public Equals(String column, String variable) {
            this(column, variable, null);
        }

        public Equals(String column, String variable, Object value) {
            super(column, "=", variable, value);
        }
    }

    class Compare implements Condition {
        private final String column;
        private final String operator;
        private final String variable;
        private final Object value;

        public Compare(String column, String operator, String variable, Object value) {
            this.column = column;
            this.operator = operator;
            this.variable = variable;
            this.value = value;
        }

        public String getColumn() {
            return column;
        }

        public String getOperator() {
            return operator;
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

    class Or extends Binary {
        public Or(Condition first, Condition second) {
            super(first, second, "OR");
        }
    }

    class And extends Binary {
        public And(Condition first, Condition second) {
            super(first, second, "AND");
        }
    }

    class Binary implements Condition {
        private final Condition first;
        private final Condition second;
        private final String operator;

        public Binary(Condition first, Condition second, String operator) {
            this.first = first;
            this.second = second;
            this.operator = operator;
        }

        public Condition getFirst() {
            return first;
        }

        public Condition getSecond() {
            return second;
        }

        public String getOperator() {
            return operator;
        }
    }

}
