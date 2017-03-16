package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Xt2400ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Xt2400ProtocolDecoder decoder = new Xt2400ProtocolDecoder(new Xt2400Protocol());

        decoder.setConfig("\n:wycfg pcr[0] 000f01030406070809570a131217141005\n");

        verifyPosition(decoder, binary(
                "0009c4fb9b0b58a771e4020742d9f8f1c4c300bc0000000011077c0015000000000001"));

    }

}
