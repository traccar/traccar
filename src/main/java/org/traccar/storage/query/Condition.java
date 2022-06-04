/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.storage.query;

import org.traccar.model.GroupedModel;

import java.util.List;

public interface Condition {

    static Condition merge(List<Condition> conditions) {
        Condition result = null;
        var iterator = conditions.iterator();
        if (iterator.hasNext()) {
            result = iterator.next();
            while (iterator.hasNext()) {
                result = new Condition.And(result, iterator.next());
            }
        }
        return result;
    }

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

    class Permission implements Condition {
        private final Class<?> ownerClass;
        private final long ownerId;
        private final Class<?> propertyClass;
        private final long propertyId;
        private final boolean excludeGroups;

        private Permission(
                Class<?> ownerClass, long ownerId, Class<?> propertyClass, long propertyId, boolean excludeGroups) {
            this.ownerClass = ownerClass;
            this.ownerId = ownerId;
            this.propertyClass = propertyClass;
            this.propertyId = propertyId;
            this.excludeGroups = excludeGroups;
        }

        public Permission(Class<?> ownerClass, long ownerId, Class<?> propertyClass) {
            this(ownerClass, ownerId, propertyClass, 0, false);
        }

        public Permission(Class<?> ownerClass, Class<?> propertyClass, long propertyId) {
            this(ownerClass, 0, propertyClass, propertyId, false);
        }

        public Permission excludeGroups() {
            return new Permission(this.ownerClass, this.ownerId, this.propertyClass, this.propertyId, true);
        }

        public Class<?> getOwnerClass() {
            return ownerClass;
        }

        public long getOwnerId() {
            return ownerId;
        }

        public Class<?> getPropertyClass() {
            return propertyClass;
        }

        public long getPropertyId() {
            return propertyId;
        }

        public boolean getIncludeGroups() {
            boolean ownerGroupModel = GroupedModel.class.isAssignableFrom(ownerClass);
            boolean propertyGroupModel = GroupedModel.class.isAssignableFrom(propertyClass);
            return (ownerGroupModel || propertyGroupModel) && !excludeGroups;
        }
    }

    class LatestPositions implements Condition {
        private final long deviceId;

        public LatestPositions(long deviceId) {
            this.deviceId = deviceId;
        }

        public LatestPositions() {
            this(0);
        }

        public long getDeviceId() {
            return deviceId;
        }
    }

}
