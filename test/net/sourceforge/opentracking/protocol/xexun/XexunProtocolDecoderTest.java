package net.sourceforge.opentracking.protocol.xexun;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import org.junit.Test;
import net.sourceforge.opentracking.Device;
import net.sourceforge.opentracking.Position;
import net.sourceforge.opentracking.DataManager;
import static org.junit.Assert.*;

public class XexunProtocolDecoderTest {

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

        String testMsg =
                "GPRMC,150120.000,A,3346.4463,S,15057.3083,E,0.0,117.4,010911,,,A*76,F," +
                "imei:351525010943661,";
                //"GPRMC,010203.000,A,0102.0003,N,00102.0003,E,1.02,1.02,010203,,,A*00,F,, " +
                //"imei:10000000000000,";
                //"GPRMC,233842.000,A,5001.3060,N,01429.3243,E,0.00,,210211,,,A*74,F," +
                //"imei:354776030495631,";
                //1103100803,+79629503178,
                //"GPRMC,080303.000,A,5546.7313,N,03738.6005,E,0.56,160.13,100311,,,A*6A,L,imei:354778030461167,";

        XexunProtocolDecoder decoder = new XexunProtocolDecoder(new TestDataManager(), 0);
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
