package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import static org.traccar.helper.DecoderVerifier.verify;

public class AplicomProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        AplicomProtocolDecoder decoder = new AplicomProtocolDecoder(null);
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "44c20144563508385a009500b09f7700c0555ea99e555ea9b103bb569f01883ff50b00002a30f000000000000013074349460108010007555ea99e000000000000003f0000ae017605b3ff00000000010000006700d900d500000003000000000000006700d900d500000087002500c4ff0000435020150000000040512001000000000000020d0000030d0000040c0000040d0000050c0000050d0000058c0000060c"))));

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
