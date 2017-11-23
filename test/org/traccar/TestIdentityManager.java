package org.traccar;

import org.traccar.database.*;
import org.traccar.model.*;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Map;

public final class TestIdentityManager implements IdentityManager {

    private static Map<Long, Device> devicesById = new ConcurrentHashMap<>();

    private static final long TEST_DEVICE_ID_START = 100;
    private static final String TEST_DEVICE_DEFAULT_UNIQUE_ID_PREFIX = "555";
    private static long newDeviceId = TEST_DEVICE_ID_START;

    public TestIdentityManager() {
    }

    public static Device createTestDevice() {
        Device device = new Device();
        device.setId(newDeviceId);
        device.setName("TestDevice" + newDeviceId);
        device.setUniqueId(TEST_DEVICE_DEFAULT_UNIQUE_ID_PREFIX + newDeviceId);
        devicesById.put(newDeviceId, device);
        newDeviceId++;
        return device;
    }

    private Device createDefaultTestDevice() {
        Device device = new Device();
        device.setId(1);
        device.setName("test");
        device.setUniqueId("123456789012345");
        return device;
    }

    @Override
    public Device getById(long id) {
        return id < TEST_DEVICE_ID_START ? createDefaultTestDevice() : devicesById.get(id);
    }

    @Override
    public Device getByUniqueId(String uniqueId) {
        if (uniqueId.length() > TEST_DEVICE_DEFAULT_UNIQUE_ID_PREFIX.length()
                && uniqueId.substring(0, TEST_DEVICE_DEFAULT_UNIQUE_ID_PREFIX.length())
                .equals(TEST_DEVICE_DEFAULT_UNIQUE_ID_PREFIX)) {
            try {
                long id = Long.parseLong(
                        uniqueId.substring(TEST_DEVICE_DEFAULT_UNIQUE_ID_PREFIX.length(), uniqueId.length()));
                if (id >= TEST_DEVICE_ID_START) {
                    return devicesById.get(id);
                }
            } catch (NumberFormatException e) {}
        }
        return createDefaultTestDevice();
    }

    @Override
    public Position getLastPosition(long deviceId) {
        return null;
    }

    @Override
    public boolean isLatestPosition(Position position) {
        return true;
    }

    @Override
    public boolean lookupAttributeBoolean(
            long deviceId, String attributeName, boolean defaultValue, boolean lookupConfig) {
        if (deviceId < TEST_DEVICE_ID_START) {
            return defaultValue;
        }

        Object result = devicesById.get(deviceId).getAttributes().get(attributeName);
        if (result != null) {
            return result instanceof String ? Boolean.parseBoolean((String) result) : (Boolean) result;
        }
        return defaultValue;
    }

    @Override
    public String lookupAttributeString(
            long deviceId, String attributeName, String defaultValue, boolean lookupConfig) {
        if (deviceId < TEST_DEVICE_ID_START) {
            return "alarm,result";
        }

        Object result = devicesById.get(deviceId).getAttributes().get(attributeName);
        return result != null ? (String) result : defaultValue;
    }

    @Override
    public int lookupAttributeInteger(
            long deviceId, String attributeName, int defaultValue, boolean lookupConfig) {
        if (deviceId < TEST_DEVICE_ID_START) {
            return defaultValue;
        }

        Object result = devicesById.get(deviceId).getAttributes().get(attributeName);
        if (result != null) {
            return result instanceof String ? Integer.parseInt((String) result) : ((Number) result).intValue();
        }
        return defaultValue;
    }

    @Override
    public long lookupAttributeLong(
            long deviceId, String attributeName, long defaultValue, boolean lookupConfig) {
        if (deviceId < TEST_DEVICE_ID_START) {
            return defaultValue;
        }

        Object result = devicesById.get(deviceId).getAttributes().get(attributeName);
        if (result != null) {
            return result instanceof String ? Long.parseLong((String) result) : ((Number) result).longValue();
        }
        return defaultValue;
    }

}
