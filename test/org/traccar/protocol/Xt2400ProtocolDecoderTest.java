package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Xt2400ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Xt2400ProtocolDecoder decoder = new Xt2400ProtocolDecoder(new Xt2400Protocol());

        decoder.setConfig("\n:wycfg pcr[0] 001001030406070809570a13121714100565\n");

        verifyPosition(decoder, binary(
                "000a344f1f0259766ae002074289f8f1c4b200e80000026712068000130000029300883559464255524845364650323433343235"));

        decoder.setConfig("\n:wycfg pcr[0] 000f01030406070809570a131217141005\n");

        verifyPosition(decoder, binary(
                "0009c4fb9b0b58a771e4020742d9f8f1c4c300bc0000000011077c0015000000000001"));

    }

}
