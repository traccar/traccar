package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

public class AplicomProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        AplicomProtocolDecoder decoder = new AplicomProtocolDecoder(null);

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "44C20146B710C158DA009500B09F7700C054CA0EA454CA0EA403BE0BF6015D706B070000142A600000000000000002434946010801000754CA0EA4000000000000008400000000000000000000000000000000300000FE00FE0000000000000000000000000000000000000000000000000000000000000000000040502035000000000000020D0000030D0000040C0000040D0000050C0000050D0000058C0000060C"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "44C20146B710C158DA002100B09F0700C054CA0EA254CA0E9C03BE0BF6015D7069070000142A600000000000000001"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "44C20143720729D6840043031fff7191C0450ef906450ef90603b20b8003b20b80066465b3870ce30f010ce30ce3003200001520000000030aa200003b13000000320300000bcb17acff0099000186a002"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "440129D684002b0700C0450ef906450ef90603b20b8003b20b80066465b3870ce30f010ce30ce300003b130300000bcb170a"))));

    }

}
