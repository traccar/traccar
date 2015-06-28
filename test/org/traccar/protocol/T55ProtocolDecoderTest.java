package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class T55ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        T55ProtocolDecoder decoder = new T55ProtocolDecoder(new T55Protocol());
        
        assertNull(decoder.decode(null, null, "$GPFID,ID123456ABC"));

        assertNull(decoder.decode(null, null, "$PGID,359853000144328*0F"));

        assertNull(decoder.decode(null, null, "$PCPTI,CradlePoint Test,184453,184453.0,6F*57"));
        
        assertNull(decoder.decode(null, null, "IMEI 351467108700000"));

        verify(decoder.decode(null, null,
                "$GPRMC,094907.000,A,6000.5332,N,03020.5192,E,1.17,60.26,091111,,*33"));

        verify(decoder.decode(null, null,
                "$GPRMC,115528.000,A,6000.5432,N,03020.4948,E,,,091111,,*06"));
        
        verify(decoder.decode(null, null,
                "$GPRMC,064411.000,A,3717.240078,N,00603.046984,W,0.000,1,010313,,,A*6C"));
        
        verify(decoder.decode(null, null,
                "$GPGGA,000000.0,4337.200755,N,11611.955704,W,1,05,3.5,825.5,M,-11.0,M,,*6F"));
        
        verify(decoder.decode(null, null,
                "$GPGGA,000000,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47"));
        
        verify(decoder.decode(null, null,
                "$GPRMA,V,0000.00,S,00000.00,E,,,00.0,000.,11.,E*7"));
        
        verify(decoder.decode(null, null,
                "$TRCCR,20140101001122.333,V,60.0,-100.0,1.1,2.2,3.3,4.4,*00"));
        
        verify(decoder.decode(null, null,
                "$TRCCR,20140111000000.000,A,60.000000,60.000000,0.00,0.00,0.00,50,*3a"));
        
        verify(decoder.decode(null, null,
                "$GPRMC,125735.000,A,6010.34349,N,02445.72838,E,1.0,101.7,050509,6.9,W,A*1F"));

        verify(decoder.decode(null, null,
                "$GPGGA,000000.000,6010.34349,N,02445.72838,E,1,05,1.7,0.9,M,35.1,M,,*59"));
        
        verify(decoder.decode(null, null,
                "123456789$GPGGA,000000.000,4610.1676,N,00606.4586,E,0,00,4.3,0.0,M,50.7,M,,0000*59"));
        
        verify(decoder.decode(null, null,
                "123456789$GPRMC,155708.252,V,4610.1676,N,00606.4586,E,000.0,000.0,060214,,,N*76"));
        
        verify(decoder.decode(null, null,
                "990000561287964,$GPRMC,213516.0,A,4337.216791,N,11611.995877,W,0.0,335.4,181214,,,A * 72"));

    }

}
