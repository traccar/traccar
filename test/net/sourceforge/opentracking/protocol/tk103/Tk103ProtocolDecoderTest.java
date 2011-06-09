package net.sourceforge.opentracking.protocol.tk103;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.GregorianCalendar;
import java.util.List;
import org.junit.Test;
import net.sourceforge.opentracking.Device;
import net.sourceforge.opentracking.Position;
import net.sourceforge.opentracking.DataManager;
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

        String testMsg1 = "(035988863964BP05000035988863964110524A4241.7977N02318.7561E000.0123536356.5100000000L000946BB";
        Tk103ProtocolDecoder decoder = new Tk103ProtocolDecoder(new TestDataManager(), 0);
        Position position = (Position) decoder.decode(null, null, testMsg1);

        assertEquals(true, position.getValid());
    }

}
