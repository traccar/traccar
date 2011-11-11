package net.sourceforge.opentracking.protocol.t55;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import org.junit.Test;
import net.sourceforge.opentracking.Device;
import net.sourceforge.opentracking.Position;
import net.sourceforge.opentracking.DataManager;
import static org.junit.Assert.*;

public class T55ProtocolDecoderTest {

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
        
        T55ProtocolDecoder decoder = new T55ProtocolDecoder(new TestDataManager(), 0);
        
        assertNull(decoder.decode(null, null, "$PGID,359853000144328*0F"));

        assertNotNull(decoder.decode(null, null,
                "$GPRMC,094907.000,A,6000.5332,N,03020.5192,E,1.17,60.26,091111,,*33"));

        assertNotNull(decoder.decode(null, null,
                "$GPRMC,115528.000,A,6000.5432,N,03020.4948,E,,,091111,,*06"));

    }

}
