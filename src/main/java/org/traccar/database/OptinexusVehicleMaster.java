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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.OptinexusOutboxEvent;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The vehicle master data OptiRadar shares with the other platforms (OptiNexus docs/integration/MASTER_DATA_SYNC.md).
 * OptiRadar owns the telematics group of a vehicle (which device reports for it) and publishes it as
 * {@code optiradar.vehicle.upserted} through the same outbox as the device events, so nothing is lost while OptiNexus
 * is down.
 *
 * <p>A device is a vehicle for the other platforms only when it carries a registration number (the device attribute
 * {@code registrationNumber}, or {@code plate}) and belongs to a tenant. The registration number is normalised to the
 * shared identity key (upper case, letters and digits only). Nothing is ever read back: OptiRadar owns nothing that
 * depends on the data of another platform, so no event can loop.
 */
@Singleton
public class OptinexusVehicleMaster {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptinexusVehicleMaster.class);

    static final String EVENT_KEY = "optiradar.vehicle.upserted";

    private static final int MAX_GROUP_DEPTH = 10;

    private static final AtomicLong LAST_VERSION = new AtomicLong();

    private final Storage storage;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String tenantAttribute;

    @Inject
    public OptinexusVehicleMaster(Config config, Storage storage, ObjectMapper objectMapper) {
        this.storage = storage;
        this.objectMapper = objectMapper;
        this.enabled = config.getBoolean(Keys.OPTINEXUS_EVENTS_ENABLE);
        this.tenantAttribute = config.getString(Keys.OPENID_TENANT_GROUP_ATTRIBUTE);
    }

    /**
     * The OptiNexus tenant of the device: the identifier on its group, else on the nearest ancestor group. The groups are
     * read from storage, not from the cache: a device that was just saved is not in the cache yet. Anything that is not a
     * UUID counts as no tenant.
     */
    public String tenantOf(Device device) throws StorageException {
        long groupId = device.getGroupId();
        for (int depth = 0; groupId > 0 && depth < MAX_GROUP_DEPTH; depth++) {
            Group group = storage.getObject(Group.class, new Request(
                    new Columns.All(), new Condition.Equals("id", groupId)));
            if (group == null) {
                return null;
            }
            String value = group.getString(tenantAttribute);
            if (value != null && !value.isBlank()) {
                try {
                    return UUID.fromString(value.trim()).toString().toLowerCase(Locale.ROOT);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
            groupId = group.getGroupId();
        }
        return null;
    }

    /** The identity key shared by all platforms: upper case, letters and digits only. */
    public static String key(String registration) {
        return registration == null ? "" : registration.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    /** The registration number a device carries, or null: only an explicit attribute counts, never the name. */
    public static String registrationOf(Device device) {
        for (String attribute : new String[] {"registrationNumber", "plate"}) {
            String value = device.getString(attribute);
            if (value != null && !key(value).isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static long nextVersion() {
        return LAST_VERSION.updateAndGet(last -> Math.max(System.currentTimeMillis(), last + 1));
    }

    /**
     * Queues the telematics group of the device's vehicle. Never throws: sharing is a side channel and must not break
     * saving a device.
     *
     * @return whether an event was queued
     */
    public boolean publish(Device device) {
        if (!enabled || device == null) {
            return false;
        }
        try {
            String registration = registrationOf(device);
            String tenantId = registration != null ? tenantOf(device) : null;
            if (registration == null || tenantId == null) {
                return false;
            }
            queue(device, registration, tenantId);
            return true;
        } catch (Exception e) {
            LOGGER.warn("Vehicle master data of device {} not queued", device.getId(), e);
            return false;
        }
    }

    /** Queues every device of a tenant that has a registration number (first start-up of the sync, or an outage). */
    public int publishAll() throws StorageException {
        int count = 0;
        for (Device device : storage.getObjects(Device.class, new Request(new Columns.All()))) {
            count += publish(device) ? 1 : 0;
        }
        return count;
    }

    private void queue(Device device, String registration, String tenantId) throws Exception {
        long version = nextVersion();
        Date now = new Date();

        Map<String, Object> telematics = new LinkedHashMap<>();
        telematics.put("device_ref", device.getUniqueId());
        telematics.put("device_name", device.getName());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("key", key(registration));
        payload.put("registration_number", registration);
        payload.put("version", version);
        payload.put("changed_at", now.toInstant().toString());
        payload.put("telematics", telematics);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("aggregate_type", "vehicle");
        data.put("aggregate_id", String.valueOf(device.getId()));
        data.put("correlation", Map.of("unique_id", device.getUniqueId()));
        data.put("payload", payload);

        OptinexusOutboxEvent row = new OptinexusOutboxEvent();
        row.setEventUuid(UUID.randomUUID().toString());
        row.setEventKey(EVENT_KEY);
        row.setTenantId(tenantId);
        row.setDeviceId(device.getId());
        row.setOccurredAt(now);
        row.setPayload(objectMapper.writeValueAsString(data));
        row.setId(storage.addObject(row, new Request(new Columns.Exclude("id"))));
    }

}
