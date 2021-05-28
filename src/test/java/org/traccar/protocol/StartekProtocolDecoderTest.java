package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class StartekProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new StartekProtocolDecoder(null);

        verifyAttribute(decoder, text(
                "&&a152,860262050010565,000,53,8F5300,210528015706,A,-38.229746,145.043446,6,1.5,0,285,84,2102994,505|1|306E|082D6101,31,0000003D,02,02,04C0|01A0|0000|0000,1,,DC"),
                Position.KEY_DRIVER_UNIQUE_ID, "8F5300");

        verifyPosition(decoder, text(
                "&&>141,860262050010565,000,36,,210407094323,V,-38.229711,145.043161,0,0.0,0,0,0,14222,505|1|306E|082D6115,24,00000039,00,00,04C0|0164|0000|0000,1,,41"));

        verifyPosition(decoder, text(
                "&&A147,021104023195429,000,0,,180106093046,A,22.646430,114.065730,8,0.9,54,86,76,326781,460|0|27B3|0EA7,27,0000000F,02,01,04E2|018C|01C8|0000,1,0104B0,01013D|02813546"));

        verifyPosition(decoder, text(
                "&&y139,860262050009146,000,0,,210323131512,A,22.678655,114.046223,14,1.1,0,231,71,5,460|0|249F|0099C257,28,0000003D,00,00,0493|0199|0000|0000,1,,33"));

    }

}
