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

        var decoder = inject(new PacificTrackProtocolDecoder(null));

        verifyAttributes(decoder, binary(
                "FB80019702808835275309000091108181B2C08F0143000E10000000010000001400010192DF0143288063810A8202835584D285B486E68780882D89C38A788BCE8C3A8D3C8E418F809073A008ACA16600A225A0C0000F4240C10003DF2CC200004E20C3004428C0C4000008C6C5000316A4E011314334424A57464758444C35333137373302A086AB569DFE110E02A8811203FF81000190820100"));

        verifyAttributes(decoder, binary(
                "fb80c88181b00280883592151012618820b18b1f123340f004c90001300301928a0080008100c00000000091971c0b0417020d074df0ec03c242550b20081d0c009a0601a1855571a30000"));

        verifyAttributes(decoder, binary(
                "fb82e80280883527530900009110818202c0909308990b122519076138fc03b3480205a3e80003a0834dd19fb08112c08f0143000e020000000100000014000101929f806328c0000f4240810a858ce011314334424a57464758444c3533313737330190868102100828cf"));

        verifyAttributes(decoder, binary(
                "FB80B48181B20192AE86E68780882D89BB8A648BCEA008ACA16600A20380C10003DF2CC200004E20C3004428C0C4000008C6C5000316A4"));

    }

}
