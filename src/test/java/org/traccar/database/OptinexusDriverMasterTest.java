package org.traccar.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Driver;
import org.traccar.model.OptinexusOutboxEvent;
import org.traccar.storage.MemoryStorage;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OptinexusDriverMasterTest {

    private static final String TENANT_A = "0b1f6c1e-6a3a-4c5e-9d52-6f0f6a1b2c01";
    private static final String TENANT_B = "0b1f6c1e-6a3a-4c5e-9d52-6f0f6a1b2c02";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Storage storage;
    private OpenIdTenantLinker linker;

    @BeforeEach
    public void setUp() throws StorageException {
        storage = new MemoryStorage();
        linker = mock(OpenIdTenantLinker.class);
        when(linker.tenantsOf(anyLong())).thenReturn(Set.of(TENANT_A));
    }

    private OptinexusDriverMaster master(boolean enabled) {
        Config config = new Config();
        config.setString(Keys.OPTINEXUS_EVENTS_ENABLE, String.valueOf(enabled));
        return new OptinexusDriverMaster(config, storage, objectMapper, linker);
    }

    private Driver driver(long id, String name, String license) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setName(name);
        driver.setUniqueId("TAG" + id);
        if (license != null) {
            driver.set("licenseNumber", license);
        }
        return driver;
    }

    private List<OptinexusOutboxEvent> rows() throws StorageException {
        return storage.getObjects(OptinexusOutboxEvent.class, new Request(new Columns.All()));
    }

    @Test
    public void queuesTheTagWithTheNormalisedLicenceKeyForTheTenantOfTheUser() throws Exception {
        assertTrue(master(true).publish(driver(5, "Budi", "sim-1234 56"), 7));

        OptinexusOutboxEvent row = rows().get(0);
        assertEquals("optiradar.driver.upserted", row.getEventKey());
        assertEquals(TENANT_A, row.getTenantId());
        JsonNode data = objectMapper.readTree(row.getPayload());
        assertEquals("driver", data.path("aggregate_type").asText());
        JsonNode payload = data.path("payload");
        assertEquals("SIM123456", payload.path("key").asText());
        assertEquals("sim-1234 56", payload.path("license_number").asText());
        assertEquals("Budi", payload.path("name").asText());
        assertEquals("TAG5", payload.path("telematics").path("tag_id").asText());
        assertFalse(payload.has("asset"));
    }

    @Test
    public void sharesNothingWithoutALicenceNumberAUniqueTenantOrTheSwitch() throws Exception {
        assertFalse(master(true).publish(driver(5, "Budi", null), 7));
        assertFalse(master(false).publish(driver(6, "Andi", "SIM1"), 7));

        when(linker.tenantsOf(anyLong())).thenReturn(Set.of());
        assertFalse(master(true).publish(driver(7, "Admin made", "SIM2"), 1));
        when(linker.tenantsOf(anyLong())).thenReturn(Set.of(TENANT_A, TENANT_B));
        assertFalse(master(true).publish(driver(8, "Two tenants", "SIM3"), 2));
        when(linker.tenantsOf(anyLong())).thenReturn(Set.of("not-a-uuid"));
        assertFalse(master(true).publish(driver(9, "Bad tenant", "SIM4"), 3));

        assertEquals(0, rows().size());
    }

    @Test
    public void versionsOnlyIncrease() throws Exception {
        OptinexusDriverMaster master = master(true);
        Driver driver = driver(5, "Budi", "SIM1");

        master.publish(driver, 7);
        master.publish(driver, 7);

        long first = objectMapper.readTree(rows().get(0).getPayload()).path("payload").path("version").asLong();
        long second = objectMapper.readTree(rows().get(1).getPayload()).path("payload").path("version").asLong();
        assertTrue(first < second);
    }

}
