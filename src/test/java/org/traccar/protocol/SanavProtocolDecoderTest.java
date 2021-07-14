package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class SanavProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new SanavProtocolDecoder(null);

        verifyPosition(decoder, text(
                "imei=353197040023431&rmc=$GPRMC,015258.000,A,2457.8101,N,12125.5393,E,0.00,0.00,210111,,*18,AUTO,0300,2.1,10,466,97,34E7,3391,74,466,9 7,3F2D,3391,65,466,97,39C9,3391,79,466,97,3F2C,3391,81,466,97,0000,00 00,83,466,97,0000,0000,85,466,97,0000,0000,85,1,24"));

        verifyPosition(decoder, text(
                "imei=1234567890&rmc=$GPRMC,091950.00,V,5300.10000,N,00900.14000,E,0.160,,200513,,,A*68,STOP,V3.872;67%,S4,H8.3,D2.38"));
        
        verifyPosition(decoder, text(
                "imei=352024028982787&rmc=$GPRMC,103048.000,A,4735.0399,N,01905.2895,E,0.00,0.00,171013,,*05,AUTO-4095mv"),
                position("2013-10-17 10:30:48.000", true, 47.58400, 19.08816));

        verifyPosition(decoder, text(
                "imei:352024028980000rmc:$GPRMC,093604.354,A,4735.0862,N,01905.2146,E,0.00,0.00,171013,,*09,AUTO-4103mv"));

        verifyPosition(decoder, text(
                "imei:352024027800000rmc:$GPRMC,000025.000,A,4735.0349,N,01905.2899,E,0.00,202.97,171013,,*03,3950mV,AUTO"));

        verifyPosition(decoder, text(
                "imei:352024020976845rmc:$GPRMC,000201.000,A,4655.7043,N,01941.3796,E,0.54,159.14,171013,,,A*65,AUTO"));

        verifyPosition(decoder, text(
                "imei:352024020976845rmc:$GPRMC,000201.000,A,4655.7043,N,01941.3796,E,0.54,159.14,171013,,,A*65,AUTO"));

    }

}
