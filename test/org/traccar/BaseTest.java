package org.traccar;

import org.traccar.database.IdentityManager;
import org.traccar.model.Device;
import org.traccar.model.Position;

public class BaseTest {
    
    static {
        Context.init(new IdentityManager() {

            private Device createDevice() {
                Device device = new Device();
                device.setId(1);
                device.setName("test");
                device.setUniqueId("123456789012345");
                return device;
            }

            @Override
            public Device getDeviceById(long id) {
                return createDevice();
            }

            @Override
            public Device getDeviceByUniqueId(String uniqueId) {
                return createDevice();
            }
            
            @Override
            public Position getLastPosition(long deviceId) {
                return null;
            }
            
            @Override
            public boolean isLatestPosition(Position position) {
                return true;
            }

        });
    }

}
