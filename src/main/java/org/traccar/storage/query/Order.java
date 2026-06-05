/*
 * Copyright 2022 - 2026 Anton Tananaev (anton@traccar.org)
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

    private final String[] columns;
    private final boolean descending;
    private final int limit;
    private final int offset;

    public Order(String... columns) {
        this(false, 0, 0, columns);
    }

    public Order(boolean descending, int limit, String... columns) {
        this(descending, limit, 0, columns);
    }

    public Order(boolean descending, int limit, int offset, String... columns) {
        this.columns = columns;
        this.descending = descending;
        this.limit = limit;
        this.offset = offset;
    }

    public String[] getColumns() {
        return columns;
    }

    public boolean getDescending() {
        return descending;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

}
