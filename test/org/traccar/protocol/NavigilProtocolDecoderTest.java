package org.traccar.protocol;

import java.nio.ByteOrder;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class NavigilProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        NavigilProtocolDecoder decoder = new NavigilProtocolDecoder(new NavigilProtocol());

        verifyNothing(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "01004300040020000000f60203080200e7cd0f510c0000003b00000000000000"));

        verifyPosition(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "0100b3000f0024000000f4a803080200ca0c1151ef8885f0b82e6d130400c00403000000"));

    }

}
