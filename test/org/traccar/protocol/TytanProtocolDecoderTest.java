package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

import static org.traccar.helper.DecoderVerifier.verify;

public class TytanProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        TytanProtocolDecoder decoder = new TytanProtocolDecoder(new TytanProtocol());

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "b50028080000689d215602772f00378f1b8e9fdd98005a042efb3e4102030000000402140c070200000901"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "b500280a0000689d215602772f00378f1b8e9fdd98005a042efb3e4102030000000402140c07020000da20"))));

    }

}
