package org.traccar.config;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.traccar.Config;

public class EnvVarTest {

    @Test
    public void testFormat() {
        assertEquals("DATABASE_URL", Config.getEnvVarName("database.url"));
        assertEquals("DATABASE_CHECK_CONNECTION", Config.getEnvVarName("database.checkConnection"));
        assertEquals("DATABASE_MAX_POOL_SIZE", Config.getEnvVarName("database.maxPoolSize"));
        assertEquals("DEVICE_MANAGER_LOOKUP_GROUPS_ATTRIBUTE", Config.getEnvVarName("deviceManager.lookupGroupsAttribute"));
        assertEquals("COMMAND_FALLBACK_TO_SMS", Config.getEnvVarName("command.fallbackToSms"));
        assertEquals("STATUS_TIMEOUT", Config.getEnvVarName("status.timeout"));
    }
}
