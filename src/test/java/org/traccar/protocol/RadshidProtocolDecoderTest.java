package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class RadshidProtocolDecoderTest extends ProtocolTest {

    private static final String SAMPLE_TWO_EVENTS =
            "600000000114fcfb160201e0000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a010001f3000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a0100657a";

    private static final String SAMPLE_EXTRA =
            "5200000001001e9bfd01e00100000000000000659d4c795151635b04df000000130001e275e00015896cf81e04d54a052401300e0f1e020b067f000000a33d0000000000000000000000001c6b000b29a5a514a5cdd9";

    private static final String SAMPLE_BAD_PAYLOAD =
            "600000000114fcfb16020100000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a010001f3000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a0100657a";

    private static final String SAMPLE_BAD_CRC =
            "600000000114fcfb1602010100000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a010001f3000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a0100657a";

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new RadshidProtocolDecoder(null));

        verifyDecode(decoder, binary(SAMPLE_TWO_EVENTS),
                position().attribute(Position.KEY_EVENT, 0xE0),
                position().attribute(Position.KEY_EVENT, 0xF3));

        verifyDecode(decoder, binary(SAMPLE_EXTRA),
                position().attribute(Position.KEY_EVENT, 0x01));

        verifyDecode(decoder, binary(SAMPLE_BAD_PAYLOAD));
        verifyDecode(decoder, binary(SAMPLE_BAD_CRC));
        verifyDecode(decoder, binary("6000000001"));

    }

}
