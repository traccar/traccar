package org.traccar.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Device;
import org.traccar.model.Driver;
import org.traccar.model.Group;
import org.traccar.model.Permission;
import org.traccar.model.User;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.MemoryStorage;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OptinexusVendorRecordsTest {

    private static final String TENANT_A = "0b1f6c1e-6a3a-4c5e-9d52-6f0f6a1b2c01";
    private static final String TENANT_B = "0b1f6c1e-6a3a-4c5e-9d52-6f0f6a1b2c02";
    private static final String ATTRIBUTE = "optinexusTenantId";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Storage storage;
    private OpenIdTenantLinker linker;
    private OptinexusVendorRecords records;

    @BeforeEach
    public void setUp() throws StorageException {
        storage = new MemoryStorage();
        CacheManager cacheManager = mock(CacheManager.class);
        linker = mock(OpenIdTenantLinker.class);
        when(linker.tenantsOf(anyLong())).thenReturn(Set.of());
        Config config = new Config();
        config.setString(Keys.OPTINEXUS_EVENTS_ENABLE, "true");
        OptinexusVehicleMaster vehicleMaster = new OptinexusVehicleMaster(config, storage, objectMapper);
        OptinexusDriverMaster driverMaster = new OptinexusDriverMaster(config, storage, objectMapper, linker);
        records = new OptinexusVendorRecords(storage, cacheManager, vehicleMaster, driverMaster, linker);
    }

    private Config config() {
        Config config = new Config();
        config.setString(Keys.OPTINEXUS_EVENTS_ENABLE, "true");
        return config;
    }

    private long tenantGroup(String tenantId) throws Exception {
        Group group = new Group();
        group.setName("Tenant group");
        group.set(ATTRIBUTE, tenantId);
        group.setId(storage.addObject(group, new Request(new Columns.Exclude("id"))));
        when(linker.findGroup(tenantId)).thenReturn(group);
        return group.getId();
    }

    private Device device(long groupId, String registration) throws StorageException {
        Device device = new Device();
        device.setName("Truck");
        device.setUniqueId("IMEI" + registration.hashCode());
        device.setGroupId(groupId);
        device.set("registrationNumber", registration);
        device.setId(storage.addObject(device, new Request(new Columns.Exclude("id"))));
        return device;
    }

    private Device stored(Device device) throws StorageException {
        return storage.getObjects(Device.class, new Request(new Columns.All())).stream()
                .filter(d -> d.getId() == device.getId()).findFirst().orElseThrow();
    }

    private String event(String type, String tenantId, String source, String key, long version, boolean active,
            String extra) {
        return "{\"event_type\":\"" + type + "\",\"source_system\":\"" + source + "\",\"tenant_id\":\"" + tenantId
                + "\",\"data\":{\"payload\":{\"key\":\"" + key + "\",\"version\":" + version + ",\"active\":" + active
                + ",\"vendor\":{\"code\":\"VJ\",\"name\":\"Vendor Jaya\"}" + extra + "}}}";
    }

    private String vehicle(String tenantId, String key, long version, boolean active) {
        return event("optideli.vendor_vehicle.upserted", tenantId, "optideli", key, version, active,
                ",\"transport\":{\"carrier_type_code\":\"CDD\"}");
    }

    @Test
    public void marksTheMatchingDeviceOfTheSameTenantAsAVendorsVehicle() throws Exception {
        long a = tenantGroup(TENANT_A);
        long b = tenantGroup(TENANT_B);
        Device mine = device(a, "B 5 VV");
        Device other = device(b, "B 5 VV");

        assertEquals("applied", records.handle(vehicle(TENANT_A, "B5VV", 1, true)));

        Map<String, Object> attributes = stored(mine).getAttributes();
        assertEquals("VENDOR", attributes.get("ownership"));
        assertEquals("VJ", attributes.get("vendorCode"));
        assertEquals("Vendor Jaya", attributes.get("vendorName"));
        assertEquals("CDD", attributes.get("carrierType"));
        assertEquals(true, attributes.get("vendorActive"));
        assertNull(stored(other).getAttributes().get("ownership"), "another tenant's device is never touched");
    }

    @Test
    public void aRecordThatArrivesFirstIsAppliedWhenTheDeviceIsSaved() throws Exception {
        long a = tenantGroup(TENANT_A);
        assertEquals("applied", records.handle(vehicle(TENANT_A, "B5VV", 1, true)));

        Device late = device(a, "b-5-vv");
        records.enrich(late);

        assertEquals("Vendor Jaya", stored(late).getAttributes().get("vendorName"));
    }

    @Test
    public void anOlderVersionIsStaleAndANewerOneUpdatesTheDevice() throws Exception {
        long a = tenantGroup(TENANT_A);
        Device device = device(a, "B 5 VV");
        records.handle(vehicle(TENANT_A, "B5VV", 10, true));

        assertEquals("stale", records.handle(vehicle(TENANT_A, "B5VV", 9, false)));
        assertEquals(true, stored(device).getAttributes().get("vendorActive"));
        assertEquals("applied", records.handle(vehicle(TENANT_A, "B5VV", 11, false)));
        assertEquals(false, stored(device).getAttributes().get("vendorActive"));
    }

    @Test
    public void onlyOptideliMayTellAndOnlyAboutValidEvents() throws Exception {
        long a = tenantGroup(TENANT_A);
        Device device = device(a, "B 5 VV");

        assertEquals("ignored", records.handle(event("optideli.vendor_vehicle.upserted", TENANT_A, "optifleet", "B5VV", 1, true, "")));
        assertEquals("ignored", records.handle(event("optideli.vehicle.upserted", TENANT_A, "optideli", "B5VV", 1, true, "")));
        assertThrows(IllegalArgumentException.class, () -> records.handle(vehicle("not-a-tenant", "B5VV", 1, true)));
        assertThrows(IllegalArgumentException.class, () -> records.handle(vehicle(TENANT_A, "", 1, true)));
        assertNull(stored(device).getAttributes().get("ownership"));
    }

    @Test
    public void marksTheMatchingDriverByLicenceNumberWhenItsUserBelongsToTheTenant() throws Exception {
        Driver driver = new Driver();
        driver.setName("Vendor Driver");
        driver.setUniqueId("TAG1");
        driver.set("licenseNumber", "sim 9-9");
        driver.setId(storage.addObject(driver, new Request(new Columns.Exclude("id"))));
        storage.addPermission(new Permission(User.class, 7, Driver.class, driver.getId()));
        when(linker.tenantsOf(7)).thenReturn(Set.of(TENANT_A));

        String body = event("optideli.vendor_driver.upserted", TENANT_A, "optideli", "SIM99", 1, true,
                ",\"license_number\":\"sim 9-9\",\"name\":\"Vendor Driver\"");
        assertEquals("applied", records.handle(body));

        Driver saved = storage.getObjects(Driver.class, new Request(new Columns.All())).get(0);
        assertEquals("VENDOR", saved.getAttributes().get("ownership"));
        assertEquals("Vendor Jaya", saved.getAttributes().get("vendorName"));
    }

    private String fleet(String tenantId, String plate, long version, boolean active, String tracker) {
        return event("optideli.fleet_vehicle.upserted", tenantId, "optideli", plate.toUpperCase().replaceAll("[^A-Z0-9]", ""),
                version, active, ",\"registration_number\":\"" + plate + "\"" + (tracker == null ? "" : ",\"tracker_id\":\"" + tracker + "\"")
                        + ",\"transport\":{\"carrier_type_code\":\"CDD\"}");
    }

    private Device byName(String name) throws StorageException {
        return storage.getObjects(Device.class, new Request(new Columns.All())).stream()
                .filter(d -> name.equals(d.getName())).findFirst().orElse(null);
    }

    @Test
    public void aVehicleOfTheTenantWithoutADeviceGetsAPlaceholderThatIsNotSharedAndIsReplacedByTheRealIdentifier()
            throws Exception {
        long a = tenantGroup(TENANT_A);

        assertEquals("applied", records.handle(fleet(TENANT_A, "B 7 TK", 1, true, null)));

        Device placeholder = byName("B 7 TK");
        assertEquals("BELUM-B7TK", placeholder.getUniqueId());
        assertEquals(a, placeholder.getGroupId());
        assertEquals("B 7 TK", placeholder.getAttributes().get("registrationNumber"));
        assertEquals("TENANT", placeholder.getAttributes().get("ownership"));
        assertEquals(false, placeholder.getDisabled());
        OptinexusVehicleMaster master = new OptinexusVehicleMaster(config(), storage, objectMapper);
        assertEquals(false, master.publish(placeholder), "a placeholder is not a tracker the other platforms should hear of");

        assertEquals("applied", records.handle(fleet(TENANT_A, "B 7 TK", 2, true, "358123456789012")));
        assertEquals("358123456789012", byName("B 7 TK").getUniqueId());
        assertEquals(1, storage.getObjects(Device.class, new Request(new Columns.All())).size());
    }

    @Test
    public void aKnownTrackerIdentifierMakesTheRealDeviceAtOnceAndAnInactiveVehicleIsSwitchedOff() throws Exception {
        tenantGroup(TENANT_A);

        records.handle(fleet(TENANT_A, "B 8 TK", 1, true, "IMEI-8"));
        assertEquals("IMEI-8", byName("B 8 TK").getUniqueId());

        records.handle(fleet(TENANT_A, "B 8 TK", 2, false, "IMEI-8"));
        assertEquals(true, byName("B 8 TK").getDisabled());
        records.handle(fleet(TENANT_A, "B 8 TK", 3, true, "IMEI-8"));
        assertEquals(false, byName("B 8 TK").getDisabled());
    }

    @Test
    public void anIdentifierOfAnotherTenantsDeviceIsNeverTakenOverAndAnUnregisteredDeviceOfTheSameTenantIsAdopted()
            throws Exception {
        long a = tenantGroup(TENANT_A);
        long b = tenantGroup(TENANT_B);
        Device foreign = new Device();
        foreign.setName("Foreign");
        foreign.setUniqueId("IMEI-X");
        foreign.setGroupId(b);
        foreign.setId(storage.addObject(foreign, new Request(new Columns.Exclude("id"))));
        Device own = new Device();
        own.setName("Registered by an operator");
        own.setUniqueId("IMEI-Y");
        own.setGroupId(a);
        own.setId(storage.addObject(own, new Request(new Columns.Exclude("id"))));

        records.handle(fleet(TENANT_A, "B 9 TK", 1, true, "IMEI-X"));
        assertEquals("BELUM-B9TK", byName("B 9 TK").getUniqueId());
        assertNull(byName("Foreign").getAttributes().get("ownership"));

        records.handle(fleet(TENANT_A, "B 10 TK", 1, true, "IMEI-Y"));
        Device adopted = byName("Registered by an operator");
        assertEquals("B 10 TK", adopted.getAttributes().get("registrationNumber"));
        assertEquals("TENANT", adopted.getAttributes().get("ownership"));
        assertNull(byName("B 10 TK"), "no second device is made");
    }

    @Test
    public void aTenantWithoutAGroupGetsNoDeviceAndNoError() throws Exception {
        when(linker.findGroup(TENANT_A)).thenThrow(new java.security.GeneralSecurityException("no group"));

        assertEquals("applied", records.handle(fleet(TENANT_A, "B 11 TK", 1, true, null)));
        assertEquals(0, storage.getObjects(Device.class, new Request(new Columns.All())).size());
    }

}
