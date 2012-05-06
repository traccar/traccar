package org.traccar.protocol;

import java.util.List;
import org.traccar.model.DataManager;
import org.traccar.model.Device;
import org.traccar.model.Position;

public class TestDataManager implements DataManager {

    public List getDevices() { return null; }
    public void addDevice(Device device) {}
    public void updateDevice(Device device) {}
    public void removeDevice(Device device) {}
    public Device getDeviceByImei(String imei) {
        Device device = new Device();
        device.setId(new Long(1));
        device.setImei("123456789012345");
        return device;
    }
    public List getPositions(Long deviceId) { return null; }
    public void addPosition(Position position) {}

}
