package org.traccar.protocol.xexun2;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import org.junit.Test;
import org.traccar.Device;
import org.traccar.Position;
import org.traccar.DataManager;
import static org.junit.Assert.*;

public class Xexun2ProtocolDecoderTest {

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
        
        Xexun2ProtocolDecoder decoder = new Xexun2ProtocolDecoder(new TestDataManager(), 0);

        assertNotNull(decoder.decode(null, null,
                "111111120009,+436763737552,GPRMC,120009.590,A,4639.6774,N,01418.5737,E,0.00,0.00,111111,,,A*68,F,, imei:359853000144328,04,481.2,F:4.15V,0,139,2689,232,03,2725,0576"));

    }

}
