package org.traccar.protocol;

import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.traccar.ProtocolTest;

import static org.junit.Assert.assertEquals;

public class PacificTrackProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testReadBitExt() {

        assertEquals(0x35, PacificTrackProtocolDecoder.readBitExt(
                Unpooled.wrappedBuffer(new byte[] { (byte) 0b10110101 })));

        assertEquals(0x135, PacificTrackProtocolDecoder.readBitExt(
                Unpooled.wrappedBuffer(new byte[] { (byte) 0b00000010, (byte) 0b10110101 })));
    }


    @Test
    public void testDecode() throws Exception {

        PacificTrackProtocolDecoder decoder = new PacificTrackProtocolDecoder(null);

        verifyPosition(decoder, binary(
                "fb82e80280883527530900009110818202c0909308990b122519076138fc03b3480205a3e80003a0834dd19fb08112c08f0143000e020000000100000014000101929f806328c0000f4240810a858ce011314334424a57464758444c3533313737330190868102100828cf"));

    }

}
