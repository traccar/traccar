package org.traccar.helper;

import org.traccar.database.IdentityManager;
import org.traccar.model.Device;

public class TestIdentityManager implements IdentityManager {
    
    private final Device device;
    
    public TestIdentityManager() {
        device = new Device();
        device.setId(1);
        device.setUniqueId("123456789012345");
    }

    @Override
    public Device getDeviceById(long id) {
        return device;
    }
    
    @Override
    public Device getDeviceByUniqueId(String imei) {
        return device;
    }

}
