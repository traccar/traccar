package org.traccar.helper;

import java.util.List;
import org.traccar.database.DataManager;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class TestDataManager extends DataManager {

    public TestDataManager() throws Exception {
        super(null);
    }

    @Override
    public Device getDeviceByUniqueId(String imei) {
        Device device = new Device();
        device.setId(new Long(1));
        device.setUniqueId("123456789012345");
        return device;
    }

}
