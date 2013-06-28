package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class T55ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        T55ProtocolDecoder decoder = new T55ProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNull(decoder.decode(null, null, "$PGID,359853000144328*0F"));

        assertNull(decoder.decode(null, null, "$PCPTI,CradlePoint Test,184453,184453.0,6F*57"));

        assertNotNull(decoder.decode(null, null,
                "$GPRMC,094907.000,A,6000.5332,N,03020.5192,E,1.17,60.26,091111,,*33"));

        assertNotNull(decoder.decode(null, null,
                "$GPRMC,115528.000,A,6000.5432,N,03020.4948,E,,,091111,,*06"));
        
        assertNotNull(decoder.decode(null, null,
                "$GPRMC,064411.000,A,3717.240078,N,00603.046984,W,0.000,1,010313,,,A*6C"));
        
        assertNotNull(decoder.decode(null, null,
                "$GPGGA,184453.0,4337.200755,N,11611.955704,W,1,05,3.5,825.5,M,-11.0,M,,*6F"));
        
        assertNotNull(decoder.decode(null, null,
                "$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47"));
        
        assertNotNull(decoder.decode(null, null,
                "$GPRMA,V,0000.00,S,00000.00,E,,,00.0,000.,11.,E*7"));

    }

}
