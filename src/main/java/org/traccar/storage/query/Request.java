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

public class Request {

    private final Columns columns;
    private final Condition condition;
    private final Order order;
    private final Pagination pagination;

    public Request(Columns columns) {
        this(columns, null, null, null);
    }

    public Request(Condition condition) {
        this(null, condition, null, null);
    }

    public Request(Columns columns, Condition condition) {
        this(columns, condition, null, null);
    }

    public Request(Columns columns, Order order) {
        this(columns, null, order, null);
    }
    
    public Request(Columns columns, Pagination pagination) {
        this(columns, null, null, pagination);
    }
    
    public Request(Columns columns, Order order, Pagination pagination) {
        this(columns, null, order, pagination);
    }
    
    public Request(Columns columns, Condition condition, Order order) {
    	this(columns, condition, order, null);
    }

    public Request(Columns columns, Condition condition, Pagination pagination) {
    	this(columns, condition, null, pagination);
    }

    public Request(Columns columns, Condition condition, Order order, Pagination pagination) {
        this.columns = columns;
        this.condition = condition;
        this.order = order;
        this.pagination = pagination;
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
    
    public Pagination getPagination() {
    	return pagination;
    }

}
