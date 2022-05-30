package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class TytanProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new TytanProtocolDecoder(null));

        verifyPositions(decoder, binary(
                "B500192000001405125652CA9B1A325FC98D11A9990018020118FC0D"));

        verifyPositions(decoder, binary(
                "B500197800007422125652D7AC32325FD08D11A69900180200188280"));

        verifyPositions(decoder, binary(
                "B500181000001405115652DEEB2A325FC68D11A7D00005012A2AE1"));

        verifyPositions(decoder, binary(
                "B5005690000068494F561CEAE932325FD28D11A299000702000063045532030066013567018768014B6901286B0240396C04030785986D013E7F040000A7CE81040000A76C82027EAB83080FA01068FFFF0F3C880202583156"));
        
        verifyPositions(decoder, binary(
                "b50069a00000689d315604512b32378f1a8e9fe094005a04d7c84b41020300ab250402140c0702c0006501006601006b0280646c0402883db0315604525732378f1d8e9fdd94005a04d7c84b41020300ab250402140c0702c0006501006601006b0280646c0402883db08887"));

        verifyPositions(decoder, binary(
                "b50028080000689d215602772f00378f1b8e9fdd98005a042efb3e4102030000000402140c070200000901"));

        verifyPositions(decoder, binary(
                "b500280a0000689d215602772f00378f1b8e9fdd98005a042efb3e4102030000000402140c07020000da20"));

    }

}
