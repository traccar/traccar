package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Hyn600ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Hyn600ProtocolDecoder(null));

        verifyNotNull(decoder, binary(
                "2b5250543a0062562026071962440521010b0e010000011e030f683900ff0100000000014008fdff050416fbbc433407e906050f372e02cc000a13c200009c6a16000004c50000220d010a0700000000000000000000000007e906050f373a05e123"));

        verifyNull(decoder, binary(
                "2b41434b3a00265620260719624405215349532c303130422c2c302c07e906050f370b05a323"));

    }

}
