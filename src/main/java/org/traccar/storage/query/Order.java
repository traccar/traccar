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

public class Order {

    private final String column;
    private final boolean descending;
    private final int limit;

    public Order(String column) {
        this(column, false, 0);
    }

    public Order(String column, boolean descending, int limit) {
        this.column = column;
        this.descending = descending;
        this.limit = limit;
    }

    public String getColumn() {
        return column;
    }

    public boolean getDescending() {
        return descending;
    }

    public int getLimit() {
        return limit;
    }

}
