package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class AnytrekProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new AnytrekProtocolDecoder(null));

        verifyPosition(decoder, binary(
                "78783500300086428703204121160085015111050C0A0D20C6FD24A102FF8EAC0C01001404000000FFFFFFFF131702210000000000000000000D0A"));

        verifyPosition(decoder, binary(
                "787835003000867279033457792c009801001209080a3408c81b2a7d0305b88b0c00001ccb0000000f00000002b90174f30b000000000000000d0a"));

    }

}
