package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

public class KhdProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        KhdProtocolDecoder decoder = new KhdProtocolDecoder(new KhdProtocol());

        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "2929b1000605162935b80d"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "292980002805162935140108074727801129670365336900000103ffff000082fc0000001e78091b000000360d"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "29298100280A9F9538081228160131022394301140372500000330FF0000007FFC0F00001E000000000034290D"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "29298000280A81850A120310095750005281370061190800000232F848FFBBFFFF0000001E000000000000ED0D"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "29298E00280F80815A121218203116022318461140227000720262FB00077C7FBF5600001E3C3200000000850D"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "29298200230AA2CC391205030505220285947903109550008002078400000002000000000000750D"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "29298500081DD08C22120312174026026545710312541700000000F819C839FFFF1D00001E00500000003AF90D"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "292980002822836665140825142037045343770193879200000050ffff000082fc000004b0780b170000002a0d"))));

    }

}
