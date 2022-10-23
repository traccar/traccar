package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class NavigilProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new NavigilProtocolDecoder(null));

        verifyNull(decoder, binary(
                "01004300040020000000f60203080200e7cd0f510c0000003b00000000000000"));

        verifyPosition(decoder, binary(
                "0100b3000f0024000000f4a803080200ca0c1151ef8885f0b82e6d130400c00403000000"));

    }

}
