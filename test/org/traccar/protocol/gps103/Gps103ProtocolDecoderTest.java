package org.traccar.protocol.gps103;

import java.util.List;
import org.junit.Test;
import org.traccar.Device;
import org.traccar.Position;
import org.traccar.DataManager;
import static org.junit.Assert.*;

public class Gps103ProtocolDecoderTest {

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
        
        Gps103ProtocolDecoder decoder = new Gps103ProtocolDecoder(new TestDataManager(), 0);
        
        // assertNull(decoder.decode(null, null, "##,imei:10000000000000,A"));

        assertNotNull(decoder.decode(null, null,
                "imei:10000000000000,help me,1004171910,,F,010203.000,A,0102.0003,N,00102.0003,E,1.02,"));

        assertNotNull(decoder.decode(null, null,
                "imei:353451040164707,tracker,1105182344,+36304665439,F,214418.000,A,4804.2222,N,01916.7593,E,0.37,"));

        assertNotNull(decoder.decode(null, null,
                "imei:353451042861763,tracker,1106132241,,F,144114.000,A,2301.9052,S,04909.3676,W,0.13,"));

        assertNotNull(decoder.decode(null, null,
                "imei:359587010124900,tracker,0809231929,13554900601,F,112909.397,A,2234.4669,N,11354.3287,E,0.11,321.53,"));

    }

}
