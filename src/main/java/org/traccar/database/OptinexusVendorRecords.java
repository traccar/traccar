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
    public static final String FLEET_EVENT = "optideli.fleet_vehicle.upserted";
    public static final String DRIVER_EVENT = "optideli.vendor_driver.upserted";

    public static final String APPLIED = "applied";
    public static final String STALE = "stale";
    public static final String IGNORED = "ignored";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Storage storage;
    private final CacheManager cacheManager;
    private final OptinexusVehicleMaster vehicles;
    private final OptinexusDriverMaster drivers;
    private final OpenIdTenantLinker linker;

    @Inject
    public OptinexusVendorRecords(
            Storage storage, CacheManager cacheManager, OptinexusVehicleMaster vehicles, OptinexusDriverMaster drivers,
            OpenIdTenantLinker linker) {
        this.storage = storage;
        this.cacheManager = cacheManager;
        this.vehicles = vehicles;
        this.drivers = drivers;
        this.linker = linker;
    }

    public static boolean handles(String eventType) {
        return VEHICLE_EVENT.equals(eventType) || FLEET_EVENT.equals(eventType) || DRIVER_EVENT.equals(eventType);
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
        String kind = DRIVER_EVENT.equals(type) ? OptinexusVendorRecord.DRIVER : OptinexusVendorRecord.VEHICLE;

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
        keep.put("ownership", FLEET_EVENT.equals(type) ? "TENANT" : "VENDOR");
        keep.put("plate", payload.path("registration_number").asText(""));
        keep.put("trackerId", payload.path("tracker_id").asText("").trim());
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
            boolean found = false;
            for (Device device : storage.getObjects(Device.class, new Request(new Columns.All()))) {
                if (key.equals(OptinexusVehicleMaster.key(OptinexusVehicleMaster.registrationOf(device)))
                        && tenantId.equals(vehicles.tenantOf(device))) {
                    apply(device, record, Device.class);
                    syncDevice(device, record);
                    found = true;
                }
            }
            if (!found) {
                createPlaceholder(tenantId, key, record);
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
            String tenantId = registration != null ? vehicles.tenantOf(device) : null;
            OptinexusVendorRecord record = tenantId == null ? null
                    : find(tenantId, OptinexusVendorRecord.VEHICLE, OptinexusVehicleMaster.key(registration));
            if (record != null) {
                apply(device, record, Device.class);
                syncDevice(device, record);
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

    /**
     * A vehicle OptiDeli knows has no device here yet: make one in the tenant group so the vehicle is there to be found. Its identifier is
     * the identifier the tracker reports when OptiDeli has it, else a placeholder an operator replaces later. Nothing is made when the
     * tenant has no group; an identifier that already belongs to a device of another tenant is never taken over.
     */
    private void createPlaceholder(String tenantId, String key, OptinexusVendorRecord record) throws StorageException {
        try {
            JsonNode kept = MAPPER.readTree(record.getPayload());
            String plate = kept.path("plate").asText("");
            if (plate.isBlank()) {
                return;
            }
            org.traccar.model.Group group;
            try {
                group = linker.findGroup(tenantId);
            } catch (java.security.GeneralSecurityException e) {
                LOGGER.info("Tenant {} has no group here, no device made for vehicle {}", tenantId, key);
                return;
            }
            if (group == null) {
                return;
            }
            String tracker = kept.path("trackerId").asText("");
            String uniqueId = OptinexusVehicleMaster.PLACEHOLDER_PREFIX + key;
            if (!tracker.isEmpty()) {
                Device owner = byUniqueId(tracker);
                if (owner == null) {
                    uniqueId = tracker;
                } else if (tenantId.equals(vehicles.tenantOf(owner))) {
                    adopt(owner, plate);
                    apply(owner, record, Device.class);
                    syncDevice(owner, record);
                    return;
                } else {
                    LOGGER.warn("The tracker identifier of vehicle {} belongs to a device of another tenant, placeholder used", key);
                }
            }
            if (byUniqueId(uniqueId) != null) {
                return; // the placeholder of this vehicle is already there
            }
            Device device = new Device();
            device.setName(plate.trim());
            device.setUniqueId(uniqueId);
            device.setGroupId(group.getId());
            device.set("registrationNumber", plate.trim());
            device.setDisabled(!kept.path("active").asBoolean(true));
            device.setId(storage.addObject(device, new Request(new Columns.Exclude("id"))));
            apply(device, record, Device.class);
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /** Keeps the device in line with the record: its tracker identifier once known, and switched off while the vehicle is inactive. */
    private void syncDevice(Device device, OptinexusVendorRecord record) throws StorageException {
        try {
            JsonNode kept = MAPPER.readTree(record.getPayload());
            boolean changed = false;
            String tracker = kept.path("trackerId").asText("");
            if (!tracker.isEmpty() && OptinexusVehicleMaster.isPlaceholder(device) && byUniqueId(tracker) == null) {
                device.setUniqueId(tracker);
                storage.updateObject(device, new Request(
                        new Columns.Include("uniqueId"), new Condition.Equals("id", device.getId())));
                changed = true;
            }
            boolean disabled = !kept.path("active").asBoolean(true);
            if (device.getDisabled() != disabled) {
                device.setDisabled(disabled);
                storage.updateObject(device, new Request(
                        new Columns.Include("disabled"), new Condition.Equals("id", device.getId())));
                changed = true;
            }
            if (changed) {
                cacheManager.invalidateObject(true, Device.class, device.getId(), ObjectOperation.UPDATE);
            }
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    private void adopt(Device device, String plate) throws StorageException {
        if (OptinexusVehicleMaster.registrationOf(device) == null) {
            device.set("registrationNumber", plate.trim());
            storage.updateObject(device, new Request(
                    new Columns.Include("attributes"), new Condition.Equals("id", device.getId())));
        }
    }

    private Device byUniqueId(String uniqueId) throws StorageException {
        return storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("uniqueId", uniqueId)));
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
        boolean changed = set(entity, "ownership", kept.path("ownership").asText("VENDOR"));
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
