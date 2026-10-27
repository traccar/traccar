package org.traccar.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.OptinexusOutboxEvent;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.MemoryStorage;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OptinexusVehicleMasterTest {

    private static final String TENANT_A = "0b1f6c1e-6a3a-4c5e-9d52-6f0f6a1b2c01";
    private static final String ATTRIBUTE = "optinexusTenantId";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Long, Device> devices = new HashMap<>();
    private final Map<Long, Group> groups = new HashMap<>();

    private Storage storage;
    private CacheManager cacheManager;

    @BeforeEach
    public void setUp() {
        storage = new MemoryStorage();
        cacheManager = mock(CacheManager.class);
        when(cacheManager.getObject(any(), anyLong())).thenAnswer(invocation -> {
            Class<?> type = invocation.getArgument(0);
            long id = invocation.getArgument(1);
            if (type == Device.class) {
                return devices.get(id);
            } else if (type == Group.class) {
                return groups.get(id);
            }
            return null;
        });
    }

    private OptinexusVehicleMaster master(boolean enabled) {
        Config config = new Config();
        config.setString(Keys.OPTINEXUS_EVENTS_ENABLE, String.valueOf(enabled));
        OptinexusEventRecorder recorder = new OptinexusEventRecorder(config, storage, cacheManager, objectMapper);
        return new OptinexusVehicleMaster(config, storage, objectMapper, recorder);
    }

    private void addTenantGroup(long id) {
        Group group = new Group();
        group.setId(id);
        group.setName("Group " + id);
        group.set(ATTRIBUTE, TENANT_A);
        groups.put(id, group);
    }

    private Device addDevice(long id, long groupId, String registration) {
        Device device = new Device();
        device.setId(id);
        device.setName("Truck " + id);
        device.setUniqueId("IMEI" + id);
        device.setGroupId(groupId);
        if (registration != null) {
            device.set("registrationNumber", registration);
        }
        devices.put(id, device);
        return device;
    }

    private List<OptinexusOutboxEvent> rows() throws StorageException {
        return storage.getObjects(OptinexusOutboxEvent.class, new Request(new Columns.All())).stream()
                .sorted(Comparator.comparingLong(OptinexusOutboxEvent::getId))
                .toList();
    }

    @Test
    public void queuesTheTelematicsGroupOfADeviceWithARegistrationNumberUnderTheNormalisedKey() throws Exception {
        addTenantGroup(1);
        Device device = addDevice(10, 1, "b 1234-xy");

        assertTrue(master(true).publish(device));

        OptinexusOutboxEvent row = rows().get(0);
        assertEquals("optiradar.vehicle.upserted", row.getEventKey());
        assertEquals(TENANT_A, row.getTenantId());
        assertEquals(OptinexusOutboxEvent.STATUS_PENDING, row.getStatus());
        JsonNode data = objectMapper.readTree(row.getPayload());
        assertEquals("vehicle", data.path("aggregate_type").asText());
        JsonNode payload = data.path("payload");
        assertEquals("B1234XY", payload.path("key").asText());
        assertEquals("b 1234-xy", payload.path("registration_number").asText());
        assertEquals("IMEI10", payload.path("telematics").path("device_ref").asText());
        assertEquals("Truck 10", payload.path("telematics").path("device_name").asText());
        assertFalse(payload.has("asset"));
        assertFalse(payload.has("transport"));
    }

    @Test
    public void sharesNothingWithoutARegistrationNumberATenantOrTheSwitch() throws Exception {
        addTenantGroup(1);
        Device named = addDevice(10, 1, null);
        named.setName("B 1234 XY");
        Device noTenant = addDevice(11, 0, "B 1 A");

        assertFalse(master(true).publish(named), "the name alone is not a registration number");
        assertFalse(master(true).publish(noTenant), "an event without a tenant has nowhere to go");
        assertFalse(master(false).publish(addDevice(12, 1, "B 2 A")), "switched off");
        assertEquals(0, rows().size());
    }

    @Test
    public void thePlateAttributeCountsToo() throws Exception {
        addTenantGroup(1);
        Device device = addDevice(10, 1, null);
        device.set("plate", "D 8101 AH");

        assertTrue(master(true).publish(device));
        assertEquals("D8101AH", objectMapper.readTree(rows().get(0).getPayload()).path("payload").path("key").asText());
    }

    @Test
    public void versionsOnlyIncrease() throws Exception {
        addTenantGroup(1);
        Device device = addDevice(10, 1, "B 1 A");
        OptinexusVehicleMaster master = master(true);

        master.publish(device);
        master.publish(device);
        master.publish(device);

        List<Long> versions = rows().stream().map(row -> {
            try {
                return objectMapper.readTree(row.getPayload()).path("payload").path("version").asLong();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).toList();
        assertEquals(3, versions.size());
        assertTrue(versions.get(0) < versions.get(1) && versions.get(1) < versions.get(2));
    }

    @Test
    public void publishAllQueuesOnlyTheDevicesThatQualify() throws Exception {
        addTenantGroup(1);
        storage.addObject(addDevice(10, 1, "B 1 A"), new Request(new Columns.Exclude("id")));
        storage.addObject(addDevice(11, 1, null), new Request(new Columns.Exclude("id")));

        int queued = master(true).publishAll();

        assertEquals(storage.getObjects(Device.class, new Request(new Columns.All())).stream()
                .filter(d -> OptinexusVehicleMaster.registrationOf(d) != null).count(), queued);
    }

}
