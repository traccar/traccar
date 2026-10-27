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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.BaseModel;
import org.traccar.model.Device;
import org.traccar.model.Driver;
import org.traccar.model.ObjectOperation;
import org.traccar.model.OptinexusVendorRecord;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The vehicles and drivers of a tenant's vendors, as OptiDeli reports them (docs/integration/MASTER_DATA_SYNC.md,
 * stage 4). A vendor's vehicle may carry a tracker that has to be monitored here, so OptiRadar is told which vendor owns
 * it; OptiFleet is not (the tenant does not maintain a vendor's vehicles). OptiDeli owns the data: OptiRadar keeps the
 * latest version per tenant and key and shows it on the matching device (registration number) or driver (licence
 * number) as the attributes {@code ownership}, {@code vendorCode}, {@code vendorName}, {@code carrierType} and
 * {@code vendorActive}. A record that arrives before its device is kept and applied when the device is saved. Nothing is
 * ever published back, and no device or driver is created from a record: OptiRadar cannot invent a tracker's identifier.
 */
@Singleton
public class OptinexusVendorRecords {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptinexusVendorRecords.class);

    public static final String VEHICLE_EVENT = "optideli.vendor_vehicle.upserted";
    public static final String DRIVER_EVENT = "optideli.vendor_driver.upserted";

    public static final String APPLIED = "applied";
    public static final String STALE = "stale";
    public static final String IGNORED = "ignored";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Storage storage;
    private final CacheManager cacheManager;
    private final OptinexusEventRecorder recorder;
    private final OptinexusDriverMaster drivers;
    private final OpenIdTenantLinker linker;

    @Inject
    public OptinexusVendorRecords(
            Storage storage, CacheManager cacheManager, OptinexusEventRecorder recorder, OptinexusDriverMaster drivers,
            OpenIdTenantLinker linker) {
        this.storage = storage;
        this.cacheManager = cacheManager;
        this.recorder = recorder;
        this.drivers = drivers;
        this.linker = linker;
    }

    public static boolean handles(String eventType) {
        return VEHICLE_EVENT.equals(eventType) || DRIVER_EVENT.equals(eventType);
    }

    /**
     * Keeps the record and applies it to the matching devices or drivers.
     *
     * @throws IllegalArgumentException if the event is not valid (a refusal that retrying cannot fix)
     * @throws StorageException if it could not be stored (worth a retry)
     */
    public synchronized String handle(String body) throws IOException, StorageException {
        JsonNode event = MAPPER.readTree(body);
        if (event == null || !event.isObject()) {
            throw new IllegalArgumentException("The event is not a JSON object");
        }
        String type = event.path("event_type").asText();
        if (!handles(type) || !"optideli".equals(event.path("source_system").asText())) {
            return IGNORED;
        }
        String tenantId;
        try {
            tenantId = UUID.fromString(event.path("tenant_id").asText("").trim()).toString().toLowerCase();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The event names no valid tenant");
        }
        JsonNode payload = event.path("data").path("payload");
        String key = payload.path("key").asText("").toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (key.isEmpty() || !payload.path("version").canConvertToLong()) {
            throw new IllegalArgumentException("The event names no key or version");
        }
        String kind = VEHICLE_EVENT.equals(type) ? OptinexusVendorRecord.VEHICLE : OptinexusVendorRecord.DRIVER;

        OptinexusVendorRecord record = find(tenantId, kind, key);
        long version = payload.path("version").asLong();
        if (record != null && record.getVersion() >= version) {
            return STALE;
        }
        Map<String, Object> keep = new LinkedHashMap<>();
        keep.put("vendorCode", payload.path("vendor").path("code").asText(""));
        keep.put("vendorName", payload.path("vendor").path("name").asText(""));
        keep.put("carrierType", payload.path("transport").path("carrier_type_code").asText(""));
        keep.put("active", payload.path("active").asBoolean(true));
        if (record == null) {
            record = new OptinexusVendorRecord();
            record.setTenantId(tenantId);
            record.setKind(kind);
            record.setRecordKey(key);
        }
        record.setVersion(version);
        record.setPayload(MAPPER.writeValueAsString(keep));
        record.setUpdatedAt(new Date());
        if (record.getId() == 0) {
            record.setId(storage.addObject(record, new Request(new Columns.Exclude("id"))));
        } else {
            storage.updateObject(record, new Request(
                    new Columns.Exclude("id"), new Condition.Equals("id", record.getId())));
        }

        if (OptinexusVendorRecord.VEHICLE.equals(kind)) {
            for (Device device : storage.getObjects(Device.class, new Request(new Columns.All()))) {
                if (key.equals(OptinexusVehicleMaster.key(OptinexusVehicleMaster.registrationOf(device)))
                        && tenantId.equals(recorder.tenantOf(device))) {
                    apply(device, record, Device.class);
                }
            }
        } else {
            for (Driver driver : storage.getObjects(Driver.class, new Request(new Columns.All()))) {
                String license = OptinexusDriverMaster.licenseOf(driver);
                if (license != null && key.equals(OptinexusVehicleMaster.key(license))
                        && drivers.tenantsOfDriver(driver).contains(tenantId)) {
                    apply(driver, record, Driver.class);
                }
            }
        }
        return APPLIED;
    }

    /** Applies the kept record of a vendor's vehicle to a device that was just saved. Never throws. */
    public void enrich(Device device) {
        try {
            String registration = OptinexusVehicleMaster.registrationOf(device);
            String tenantId = registration != null ? recorder.tenantOf(device) : null;
            OptinexusVendorRecord record = tenantId == null ? null
                    : find(tenantId, OptinexusVendorRecord.VEHICLE, OptinexusVehicleMaster.key(registration));
            if (record != null) {
                apply(device, record, Device.class);
            }
        } catch (Exception e) {
            LOGGER.warn("Vendor record not applied to device {}", device.getId(), e);
        }
    }

    /** Applies the kept record of a vendor's driver to a driver that was just saved by this user (of exactly one tenant). */
    public void enrich(Driver driver, long userId) {
        try {
            Set<String> tenants = linker.tenantsOf(userId);
            String license = OptinexusDriverMaster.licenseOf(driver);
            if (license == null || tenants.size() != 1) {
                return;
            }
            OptinexusVendorRecord record = find(tenants.iterator().next(), OptinexusVendorRecord.DRIVER,
                    OptinexusVehicleMaster.key(license));
            if (record != null) {
                apply(driver, record, Driver.class);
            }
        } catch (Exception e) {
            LOGGER.warn("Vendor record not applied to driver {}", driver.getId(), e);
        }
    }

    private OptinexusVendorRecord find(String tenantId, String kind, String key) throws StorageException {
        return storage.getObject(OptinexusVendorRecord.class, new Request(new Columns.All(), new Condition.And(
                new Condition.Equals("tenantId", tenantId),
                new Condition.And(new Condition.Equals("kind", kind), new Condition.Equals("recordKey", key)))));
    }

    private <T extends org.traccar.model.ExtendedModel> void apply(
            T entity, OptinexusVendorRecord record, Class<T> type) throws StorageException {
        try {
            applyKept(entity, record, type);
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    private <T extends org.traccar.model.ExtendedModel> void applyKept(
            T entity, OptinexusVendorRecord record, Class<T> type) throws Exception {
        JsonNode kept = MAPPER.readTree(record.getPayload());
        boolean changed = set(entity, "ownership", "VENDOR");
        changed |= set(entity, "vendorCode", kept.path("vendorCode").asText(""));
        changed |= set(entity, "vendorName", kept.path("vendorName").asText(""));
        if (entity instanceof Device) {
            changed |= set(entity, "carrierType", kept.path("carrierType").asText(""));
        }
        changed |= set(entity, "vendorActive", kept.path("active").asBoolean(true));
        if (changed) {
            storage.updateObject(entity, new Request(
                    new Columns.Include("attributes"), new Condition.Equals("id", ((BaseModel) entity).getId())));
            cacheManager.invalidateObject(true, type, entity.getId(), ObjectOperation.UPDATE);
        }
    }

    private static boolean set(org.traccar.model.ExtendedModel entity, String name, Object value) {
        if (value instanceof String text && text.isEmpty()) {
            return false; // a field the record does not carry is not an instruction to clear it
        }
        if (value.equals(entity.getAttributes().get(name))) {
            return false;
        }
        if (value instanceof Boolean flag) {
            entity.set(name, flag);
        } else {
            entity.set(name, (String) value);
        }
        return true;
    }

}
