package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class XexunProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        XexunProtocolDecoder decoder = new XexunProtocolDecoder(null);
        
        verify(decoder.decode(null, null,
                "GPRMC,043435.000,A,811.299200,S,11339.9500,E,0.93,29.52,160313,00,0000.0,A*65,F,,imei:359585014597923,"));

        verify(decoder.decode(null, null,
                "GPRMC,150120.000,A,3346.4463,S,15057.3083,E,0.0,117.4,010911,,,A*76,F,imei:351525010943661,"));

        verify(decoder.decode(null, null,
                "GPRMC,010203.000,A,0102.0003,N,00102.0003,E,1.02,1.02,010203,,,A*00,F,,imei:10000000000000,"));

        verify(decoder.decode(null, null,
                "GPRMC,233842.000,A,5001.3060,N,01429.3243,E,0.00,,210211,,,A*74,F,imei:354776030495631,"));

        verify(decoder.decode(null, null,
                "GPRMC,080303.000,A,5546.7313,N,03738.6005,E,0.56,160.13,100311,,,A*6A,L,imei:354778030461167,"));

        verify(decoder.decode(null, null,
                "GPRMC,014623.000,A,4710.8260,N,1948.1220,E,0.11,105.40,111212,00,0000.0,A*49,F,,imei:357713002048962,"));

        verify(decoder.decode(null, null,
                "GPRMC,220828.678,A,5206.1446,N,02038.2403,,0,0,160912,,,E*23,L,imei:358948012501019,"));

        verify(decoder.decode(null, null,
                "GNRMC,134418.000,A,5533.8973,N,03745.4398,E,0.00,308.85,160215,,,A*7A,F,, imei:864244028033115,"));

        verify(decoder.decode(null, null,
                "GPRMC,093341.000,A,1344.5716,N,10033.6648,E,0.00,0.00,240215,,,A*68,F,,imei:865328028306149,"));

    }

}
