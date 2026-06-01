package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class RadshidProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new RadshidProtocolDecoder(null));

        verifyPositions(decoder, binary(
                "600000000114fcfb160201e0000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a010001f3000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a0100657a"));

        verifyPositions(decoder, binary(
                "5200000001001e9bfd01e00100000000000000659d4c795151635b04df000000130001e275e00015896cf81e04d54a052401300e0f1e020b067f000000a33d0000000000000000000000001c6b000b29a5a514a5cdd9"));

    }

}
