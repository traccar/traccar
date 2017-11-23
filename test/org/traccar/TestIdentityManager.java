package org.traccar;

import org.traccar.database.IdentityManager;
import org.traccar.model.Device;
import org.traccar.model.Position;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Map;

public final class TestIdentityManager implements IdentityManager {

    private static Map<Long, Device> devicesById = new ConcurrentHashMap<>();
    private static long newDeviceId = 100;

    public TestIdentityManager() {
    }

    private static Device createDevice(long id) {
        Device device = new Device();
        device.setId(id);
        device.setName("test");
        device.setUniqueId("123456789012345");
        return device;
    }

    public static Device createTestDevice() {
        Device device = createDevice(newDeviceId);
        devicesById.put(newDeviceId++, device);
        return device;
    }

    @Override
    public Device getById(long id) {
        Device device = devicesById.get(id);
        return device != null ? device : createDevice(1);
    }

    @Override
    public Device getByUniqueId(String uniqueId) {
        return createDevice(1);
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
        Device device = devicesById.get(deviceId);
        return device != null ? device.getBoolean(attributeName) : defaultValue;
    }

    @Override
    public String lookupAttributeString(
            long deviceId, String attributeName, String defaultValue, boolean lookupConfig) {
            return "alarm,result";
    }

    @Override
    public int lookupAttributeInteger(
            long deviceId, String attributeName, int defaultValue, boolean lookupConfig) {
            return defaultValue;
    }

    @Override
    public long lookupAttributeLong(
            long deviceId, String attributeName, long defaultValue, boolean lookupConfig) {
            return defaultValue;
    }

}
