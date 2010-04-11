package net.sourceforge.opentracking.protocol.xexun;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.LinkedList;
import org.junit.Test;
import net.sourceforge.opentracking.Device;
import net.sourceforge.opentracking.Position;
import net.sourceforge.opentracking.DataManager;
import static org.junit.Assert.*;

public class XexunProtocolDecoderTest {

    private class TestDataManager implements DataManager {
        public List readDevice() {
            List deviceList = new LinkedList();

            Device device = new Device();
            device.setId(new Long(1));
            device.setImei("10000000000000");

            deviceList.add(device);

            return deviceList;
        }

        public void writePosition(Position position) {
        }
    }

    @Test
    public void testDecode() throws Exception {

        String testMsg =
                ",+10000000000," +
                "GPRMC,010203.000,A,0102.0003,N,00102.0003,E,1.02,1.02,010203,,,A*00,F,, " +
                "imei:10000000000000,00,00.0,";

        XexunProtocolDecoder decoder = new XexunProtocolDecoder(new TestDataManager());
        Position position = (Position) decoder.decode(null, null, testMsg);

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
