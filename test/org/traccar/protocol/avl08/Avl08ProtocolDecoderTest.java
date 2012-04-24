package org.traccar.protocol.avl08;

import java.util.List;
import org.junit.Test;
import org.traccar.Device;
import org.traccar.Position;
import org.traccar.DataManager;
import static org.junit.Assert.*;

public class Avl08ProtocolDecoderTest {

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
        
        Avl08ProtocolDecoder decoder = new Avl08ProtocolDecoder(new Avl08ProtocolDecoderTest.TestDataManager(), 0);

        //$$(2 Bytes) + Len(2 Bytes) + IMEI(15 Bytes) + | + AlarmType(2 Bytes) + GPRMC + | +
        //PDOP + | + HDOP + | + VDOP + | + Status(12 Bytes) + | + RTC(14 Bytes) + | + Voltage(8 Bytes)
        //+ | + ADC(8 Bytes) + | + LACCI(8 Bytes) + | + Temperature(4 Bytes) | +Mile-meter+| +Serial(4
        //Bytes) + | +RFID(10Bytes)+|+ Checksum (4 Byte) + \r\n(2 Bytes)
        
        assertNull(decoder.decode(null, null,
                "$$AE359772033395899|AA000000000000000000000000000000000000000000000000000000000000|00.0|00.0|00.0|000000000000|20090215000153|13601435|00000000|00000000|0000|0.0000|0007|2DAA"));

        assertNull(decoder.decode(null, null,
                "$$AE359772033395899|AA000000000000000000000000000000000000000000000000000000000000|00.0|00.0|00.0|00000000|20090215001204|14182037|00000000|0012D888|0000|0.0000|0016|5B51"));

        assertNull(decoder.decode(null, null,
                "$$AE359772033395899|AA00000000000000000000000000000000000000000000000000000000000|00.0|00.0|00.0|00000000000|20090215001337|14182013|00000000|0012D888|0000|0.0000|0017|346E"));

        assertNotNull(decoder.decode(null, null,
                "$$B3359772032399074|60$GPRMC,094859.000,A,3648.2229,N,01008.0976,E,0.00,,221211,,,A*79|02.3|01.3|02.0|000000000000|20111222094858|13360808|00000000|00000000|0000|0.0000|0001||A977"));

        assertNotNull(decoder.decode(null, null,
                "$$B3359772032399074|09$GPRMC,094905.000,A,3648.2229,N,01008.0976,E,0.00,,221211,,,A*71|02.1|01.3|01.7|000000000000|20111222094905|03210533|00000000|00000000|0000|0.0000|0002||FA58"));
    
        assertNotNull(decoder.decode(null, null,
                "$$B3359772032399074|AA$GPRMC,093911.000,A,3648.2146,N,01008.0977,E,0.00,,140312,,,A*7E|02.1|01.1|01.8|000000000000|20120314093910|04100057|00000000|0012D887|0000|0.0000|1128||C50E"));

        assertNotNull(decoder.decode(null, null,
                "$$B3359772032399074|AA$GPRMC,094258.000,A,3648.2146,N,01008.0977,E,0.00,,140312,,,A*7F|02.1|01.1|01.8|000000000000|20120314094257|04120057|00000000|0012D887|0000|0.0000|1136||CA32"));

        assertNotNull(decoder.decode(null, null,
                "$$B3359772032399074|AA$GPRMC,234603.000,A,3648.2179,N,01008.0962,E,0.00,,030412,,,A*74|01.8|01.0|01.5|000000000000|20120403234603|14251914|00000000|0012D888|0000|0.0000|3674||940B"));
    }

}
