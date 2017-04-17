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

            @Override
            public boolean lookupAttributeBoolean(
                    long deviceId, String attributeName, boolean defaultValue, boolean lookupConfig) {
                return false;
            }

            @Override
            public String lookupAttributeString(
                    long deviceId, String attributeName, String defaultValue, boolean lookupConfig) {
                return null;
            }

            @Override
            public int lookupAttributeInteger(
                    long deviceId, String attributeName, int defaultValue, boolean lookupConfig) {
                return 0;
            }

            @Override
            public long lookupAttributeLong(
                    long deviceId, String attributeName, long defaultValue, boolean lookupConfig) {
                return 0;
            }

        });
    }

}
