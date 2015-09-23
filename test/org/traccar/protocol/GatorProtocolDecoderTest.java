package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

public class GatorProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        GatorProtocolDecoder decoder = new GatorProtocolDecoder(new GatorProtocol());
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "24248000260009632d141121072702059226180104367500000000c04700079c0c34000ad80b00ff000a0d"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "24248100230CA23285100306145907022346901135294700000000C04001012C0E1100000021CB0D"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "2424800023c2631e00111220104909833268648703804100000000c0470000000b4e00000000550d"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "24248000260009632d141121072702059226180104367500000000c04700079c0c34000ad80b00ff000a0d"))));
        
    }

}
