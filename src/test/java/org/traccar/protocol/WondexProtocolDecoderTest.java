package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class WondexProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new WondexProtocolDecoder(null));

        verifyPosition(decoder, buffer(
                "2005010051,19990925063830,26.106181,44.440225,0,0,0,7,2,0.0,0,,,0"));

        verifyPosition(decoder, buffer(
                "2000000108,20151030145404,76.948633,43.354700,0,140,15,100,1,1325,125.4,10.5,0.0"),
                position("2015-10-30 14:54:04.000", true, 43.35470, 76.94863));

        verifyPosition(decoder, buffer(
                "2000000257,20151030145351,69.379976,53.283905,0,0,16,2,0,0,469.1,58.9,0.0"),
                position("2015-10-30 14:53:51.000", true, 53.28390, 69.37998));

        verifyPosition(decoder, buffer(
                "2000000232,20151030145206,51.166900,43.651353,0,132,11,2,0,0,0.0,0.0,0.0"));

        verifyPosition(decoder, buffer(
                "2000000259,20151030145653,69.380826,53.283890,9,10,15,2,1,695,1002.6,108.2,0.0"));

        verifyPosition(decoder, buffer(
                "1044989601,20130323074605,0.000000,90.000000,0,000,0,0,2"));

        verifyPosition(decoder, buffer(
                "123456789000001,20120101123200,130.000000,60.000000,0,000,0,0,0,0"));

        verifyPosition(decoder, buffer(
                "210000001,20070313170040,121.123456,12.654321,0,233,0,9,2,0.0,0,0.00,0.00,0"));

        verifyPosition(decoder, buffer(
                "1044989601,20130322172647,13.572583,52.401070,22,204,49,0,2"));

        verifyPosition(decoder, buffer(
                "1044989601,20130322172647,13.572583,52.401070,22,204,-49,0,2"));

        verifyPosition(decoder, buffer(
                "3997324533,20140326074908,28.797603,47.041635,0,48,0,6,2,3.90V,0"));

        verifyPosition(decoder, buffer(
                "2000000001,20140529213210,-63.179111,9.781493,0,0,54.0,8,2,0.0,0,0.01,0.01,0,0,0,0"));

        verifyNotNull(decoder, buffer(
                "$OK:VER=M7 2.003 DVB rev02c,V2"));

        verifyNotNull(decoder, buffer(
                "$OK:REBOOT"));

        verifyNotNull(decoder, buffer(
                "$ERR:GETLOCATION=1"));

        verifyNull(decoder, binary(
                "d0d70b0001ca9a3b"));

    }

}
