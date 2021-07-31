package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Xt2400ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new Xt2400ProtocolDecoder(null);

        decoder.setConfig("\n::wycfg pcr[1] 012801030405060708090a1213c8545657585a656e7d2cd055595d5e71797a7b7c7e7f80818285866b\n");

        verifyPosition(decoder, binary(
                "010ae85be10801a05d52d590030b12d1f9330be9290a0000ff10008b00000000000000000000000000000000000000000000000000000000000000000000000000003839333032363930323031303036363039373733000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000"));

        decoder.setConfig("\n:wycfg pcr[0] 001001030406070809570a13121714100565\n");

        verifyPosition(decoder, binary(
                "000a344f1f0259766ae002074289f8f1c4b200e80000026712068000130000029300883559464255524845364650323433343235"));

        decoder.setConfig("\n:wycfg pcr[0] 000f01030406070809570a131217141005\n");

        verifyPosition(decoder, binary(
                "0009c4fb9b0b58a771e4020742d9f8f1c4c300bc0000000011077c0015000000000001"));

    }

}
