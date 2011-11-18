package org.traccar.protocol.tk103;

import java.util.List;
import org.junit.Test;
import org.traccar.Device;
import org.traccar.Position;
import org.traccar.DataManager;
import static org.junit.Assert.*;

public class Tk103ProtocolDecoderTest {

    private class TestDataManager implements DataManager {
        public List getDevices() {
            return null;
        }

        public Device getDeviceByImei(String imei) {
            Device device = new Device();
            device.setId(new Long(1));
            device.setImei("10000000000000");
            return device;
        }

        public void setPosition(Position position) {
        }
    }

    @Test
    public void testDecode() throws Exception {
        
        Tk103ProtocolDecoder decoder = new Tk103ProtocolDecoder(new TestDataManager(), 0);
        
        assertNotNull(decoder.decode(null, null,
                "(035988863964BP05000035988863964110524A4241.7977N02318.7561E000.0123536356.5100000000L000946BB"));

    }

}
