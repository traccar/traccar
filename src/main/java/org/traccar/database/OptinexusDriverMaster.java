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
import org.traccar.model.Driver;
import org.traccar.model.OptinexusOutboxEvent;
import org.traccar.model.Permission;
import org.traccar.model.User;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The drivers OptiRadar shares with the other platforms (OptiNexus docs/integration/MASTER_DATA_SYNC.md, stage 4).
 * OptiDeli owns the driver; OptiRadar only knows the identification tag, so the event carries the licence number
 * (the driver attribute {@code licenseNumber}, the key) and the tag. A driver without a licence number is not shared.
 *
 * <p>A driver has no group, so its tenant is the tenant of the user who saved it: a user linked to exactly one
 * tenant. An administrator belongs to none, so what an administrator saves is not shared (the backfill finds the
 * tenant through the users the driver is linked to).
 */
@Singleton
public class OptinexusDriverMaster {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptinexusDriverMaster.class);

    static final String EVENT_KEY = "optiradar.driver.upserted";

    private static final AtomicLong LAST_VERSION = new AtomicLong();

    private final Storage storage;
    private final ObjectMapper objectMapper;
    private final OpenIdTenantLinker tenants;
    private final boolean enabled;

    @Inject
    public OptinexusDriverMaster(
            Config config, Storage storage, ObjectMapper objectMapper, OpenIdTenantLinker tenants) {
        this.storage = storage;
        this.objectMapper = objectMapper;
        this.tenants = tenants;
        this.enabled = config.getBoolean(Keys.OPTINEXUS_EVENTS_ENABLE);
    }

    /** The licence number the driver carries, or null. */
    public static String licenseOf(Driver driver) {
        String value = driver.getString("licenseNumber");
        return value != null && !OptinexusVehicleMaster.key(value).isEmpty() ? value.trim() : null;
    }

    /**
     * Queues the driver for the tenant of the user who saved it. Never throws: sharing is a side channel and must not
     * break saving a driver.
     *
     * @return whether an event was queued
     */
    public boolean publish(Driver driver, long userId) {
        if (!enabled || driver == null) {
            return false;
        }
        try {
            Set<String> userTenants = tenants.tenantsOf(userId);
            return userTenants.size() == 1 && queue(driver, userTenants.iterator().next());
        } catch (Exception e) {
            LOGGER.warn("Driver {} not queued for the other platforms", driver.getId(), e);
            return false;
        }
    }

    /** Queues every driver whose tenant can be told from the users it is linked to. */
    public int publishAll() throws StorageException {
        int count = 0;
        for (Driver driver : storage.getObjects(Driver.class, new Request(new Columns.All()))) {
            Set<String> found = new HashSet<>();
            for (Permission permission : storage.getPermissions(User.class, 0, Driver.class, driver.getId())) {
                found.addAll(tenants.tenantsOf(permission.getOwnerId()));
            }
            if (found.size() == 1) {
                try {
                    count += queue(driver, found.iterator().next()) ? 1 : 0;
                } catch (Exception e) {
                    LOGGER.warn("Driver {} not queued for the other platforms", driver.getId(), e);
                }
            }
        }
        return count;
    }

    private boolean queue(Driver driver, String tenantId) throws Exception {
        String license = licenseOf(driver);
        if (license == null || driver.getName() == null || driver.getName().isBlank()) {
            return false;
        }
        try {
            tenantId = UUID.fromString(tenantId).toString();
        } catch (IllegalArgumentException e) {
            return false; // OptiNexus identifies a tenant by UUID: anything else has nowhere to go
        }
        Date now = new Date();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("key", OptinexusVehicleMaster.key(license));
        payload.put("license_number", license);
        payload.put("name", driver.getName().trim());
        payload.put("version", LAST_VERSION.updateAndGet(last -> Math.max(System.currentTimeMillis(), last + 1)));
        payload.put("changed_at", now.toInstant().toString());
        payload.put("telematics", Map.of("tag_id", driver.getUniqueId() == null ? "" : driver.getUniqueId()));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("aggregate_type", "driver");
        data.put("aggregate_id", String.valueOf(driver.getId()));
        data.put("correlation", Map.of("unique_id", driver.getUniqueId() == null ? "" : driver.getUniqueId()));
        data.put("payload", payload);

        OptinexusOutboxEvent row = new OptinexusOutboxEvent();
        row.setEventUuid(UUID.randomUUID().toString());
        row.setEventKey(EVENT_KEY);
        row.setTenantId(tenantId);
        row.setOccurredAt(now);
        row.setPayload(objectMapper.writeValueAsString(data));
        row.setId(storage.addObject(row, new Request(new Columns.Exclude("id"))));
        return true;
    }

}
