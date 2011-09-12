package net.sourceforge.opentracking.protocol.gps103;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.GregorianCalendar;
import java.util.List;
import org.junit.Test;
import net.sourceforge.opentracking.Device;
import net.sourceforge.opentracking.Position;
import net.sourceforge.opentracking.DataManager;
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

        Calendar time = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        time.set(Calendar.HOUR, 0);
        time.set(Calendar.MINUTE, 1);
        time.set(Calendar.SECOND, 2);
        time.set(Calendar.MILLISECOND, 3);
        String x = time.getTime().toString();

        String testMsg1 = "##,imei:10000000000000,A";
        // NEW MESSAGE: imei:359587017470280,tracker,0000000000,03184511405,L,;

        String testMsg2 = //"imei:10000000000000,help me,1004171910,,F,010203.000,A,0102.0003,N,00102.0003,E,1.02,";
                //"imei:353451040164707,tracker,1105182344,+36304665439,F,214418.000,A,4804.2222,N,01916.7593,E,0.37,";
                //"imei:353451042861763,tracker,1106132241,,         F,144114.000,A,2301.9052,S,04909.3676,W,0.13,";
                "imei:359587010124900,tracker,0809231929,13554900601,F,112909.397,A,2234.4669,N,11354.3287,E,0.11,321.53,;";

        Gps103ProtocolDecoder decoder = new Gps103ProtocolDecoder(new TestDataManager(), 0);
        //assertNull(decoder.decode(null, null, testMsg1));
        Position position = (Position) decoder.decode(null, null, testMsg2);

        //Date time = new GregorianCalendar(2003, 1, 1, 1, 2, 3).getTime();
        //assertEquals(time, position.getTime());

        assertEquals(true, position.getValid());

        Double latitude = 1.0 + 2.0003 / 60.0;
        assertEquals(latitude, position.getLatitude());

        Double longitude = 1.0 + 2.0003 / 60.0;
        assertEquals(longitude, position.getLongitude());

        Double speed = 1.02;
        assertEquals(speed, position.getSpeed());

        Long deviceId = new Long(1);
        assertEquals(deviceId, position.getDeviceId());
    }

}
