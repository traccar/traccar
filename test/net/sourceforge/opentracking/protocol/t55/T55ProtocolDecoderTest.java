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

        String test1 = "$PGID,359853000144328*0F";
        
        String test2 = "$GPRMC,094907.000,A,6000.5332,N,03020.5192,E,1.17,60.26,091111,,*33";
        //$GPRMC,115528.000,A,6000.5432,N,03020.4948,E,,,091111,,*06

        T55ProtocolDecoder decoder = new T55ProtocolDecoder(new TestDataManager(), 0);
        decoder.decode(null, null, test1);

        Position position = (Position) decoder.decode(null, null, test2);

        Date time = new GregorianCalendar(2003, 1, 1, 1, 2, 3).getTime();
        assertEquals(time, position.getTime());

        assertEquals(true, position.getValid());

        Double latitude = 1.0 + 2.0003 / 60.0;
        assertEquals(latitude, position.getLatitude());

        Double longitude = 1.0 + 2.0003 / 60.0;
        assertEquals(longitude, position.getLongitude());

        Double speed = 1.02;
        assertEquals(speed, position.getSpeed());

        Double course = 1.02;
        assertEquals(course, position.getCourse());
        
        Long deviceId = new Long(1);
        assertEquals(deviceId, position.getDeviceId());
    }

}
