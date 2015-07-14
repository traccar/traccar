package org.traccar.helper;

import org.traccar.database.IdentityManager;
import org.traccar.model.Device;

public class TestIdentityManager implements IdentityManager {

    @Override
    public Device getDeviceById(long id) {
        return null;
    }
    
    @Override
    public Device getDeviceByUniqueId(String imei) {
        Device device = new Device();
        device.setId(new Long(1));
        device.setUniqueId("123456789012345");
        return device;
    }

}
