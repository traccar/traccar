/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Device;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Singleton
public final class DeviceUpdateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceUpdateManager.class);

    private final ConcurrentHashMap<Long, Map<String, Object>> pending = new ConcurrentHashMap<>();
    private final Storage storage;
    private final boolean buffered;

    @Inject
    public DeviceUpdateManager(Config config, Storage storage) {
        this.storage = storage;
        long interval = config.getLong(Keys.DATABASE_DEVICE_UPDATE_INTERVAL);
        buffered = interval > 0;
        if (buffered) {
            var scheduler = Executors.newSingleThreadScheduledExecutor(
                    Thread.ofPlatform().daemon().name("DeviceUpdateManager").factory());
            scheduler.scheduleWithFixedDelay(this::flush, interval, interval, TimeUnit.MILLISECONDS);
        }
    }

    public void update(long deviceId, Map<String, Object> values) throws StorageException {
        if (!buffered) {
            write(deviceId, values);
            return;
        }
        pending.compute(deviceId, (id, previous) -> {
            var update = previous != null ? previous : new HashMap<String, Object>();
            values.forEach((column, value) -> update.put(column,
                    value instanceof Date date ? new Date(date.getTime()) : value));
            return update;
        });
    }

    private void write(long id, Map<String, Object> values) throws StorageException {
        Device device = new Device();
        device.setId(id);
        device.setStatus((String) values.get("status"));
        device.setLastUpdate((Date) values.get("lastUpdate"));
        device.setPositionId((Long) values.getOrDefault("positionId", 0L));
        storage.updateObject(device, new Request(
                new Columns.Include(values.keySet().toArray(String[]::new)),
                new Condition.Equals("id", id)));
    }

    void flush() {
        for (long id : pending.keySet()) {
            pending.computeIfPresent(id, (key, update) -> {
                try {
                    write(id, update);
                    return null;
                } catch (Exception e) {
                    LOGGER.warn("Failed to flush device update", e);
                    return update;
                }
            });
        }
    }

}
