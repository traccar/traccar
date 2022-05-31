package org.traccar.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConfigTest {

    @Test
    public void testFormat() {
        assertEquals("DATABASE_URL", Config.getEnvironmentVariableName("database.url"));
        assertEquals("DATABASE_CHECK_CONNECTION", Config.getEnvironmentVariableName("database.checkConnection"));
        assertEquals("DATABASE_MAX_POOL_SIZE", Config.getEnvironmentVariableName("database.maxPoolSize"));
        assertEquals("DEVICE_MANAGER_LOOKUP_GROUPS_ATTRIBUTE", Config.getEnvironmentVariableName("deviceManager.lookupGroupsAttribute"));
        assertEquals("COMMAND_FALLBACK_TO_SMS", Config.getEnvironmentVariableName("command.fallbackToSms"));
        assertEquals("STATUS_TIMEOUT", Config.getEnvironmentVariableName("status.timeout"));
    }

}
