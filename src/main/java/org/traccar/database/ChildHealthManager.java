/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.database;

import org.traccar.model.ChildHealthRecord;
import org.traccar.model.Position;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Singleton
public class ChildHealthManager {

    private final Storage storage;

    @Inject
    public ChildHealthManager(Storage storage) {
        this.storage = storage;
    }

    public ChildHealthRecord addHealthRecord(ChildHealthRecord record) throws StorageException {
        record.setId(storage.addObject(record, new Request(new Columns.Exclude("id"))));
        return record;
    }

    public List<ChildHealthRecord> getHealthHistory(long childId, Date from, Date to) throws StorageException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition.Equals("childId", childId));
        if (from != null && to != null) {
            conditions.add(new Condition.Between("servertime", from, to));
        } else if (from != null) {
            conditions.add(new Condition.Compare("servertime", ">=", from));
        } else if (to != null) {
            conditions.add(new Condition.Compare("servertime", "<=", to));
        }
        return storage.getObjects(ChildHealthRecord.class, new Request(
                new Columns.All(), Condition.merge(conditions), new Order("servertime")));
    }

    public ChildHealthRecord createFromPosition(Position position, long childId) throws StorageException {
        ChildHealthRecord record = new ChildHealthRecord();
        record.setChildId(childId);
        record.setDeviceId(position.getDeviceId());
        record.setServerTime(position.getServerTime());
        record.setHeartRate(getIntegerAttribute(position.getAttributes(), Position.KEY_HEART_RATE));
        Double bodyTemp = getDoubleAttribute(position.getAttributes(), "bodyTemp");
        if (bodyTemp == null) {
            bodyTemp = getDoubleAttribute(position.getAttributes(), Position.KEY_DEVICE_TEMP);
        }
        record.setBodyTemp(bodyTemp);
        record.setSteps(getIntegerAttribute(position.getAttributes(), Position.KEY_STEPS));
        record.setSleepStatus(getStringAttribute(position.getAttributes(), "sleepStatus"));
        if (position.getId() != 0) {
            record.setRawPositionId(position.getId());
        }

        if (record.getHeartRate() == null && record.getBodyTemp() == null
                && record.getSteps() == null && record.getSleepStatus() == null) {
            return null;
        }

        return addHealthRecord(record);
    }

    private Integer getIntegerAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private Double getDoubleAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private String getStringAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (Objects.nonNull(value)) {
            return String.valueOf(value);
        }
        return null;
    }
}
