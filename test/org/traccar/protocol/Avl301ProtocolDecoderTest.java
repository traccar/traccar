package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;

public class Avl301ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Avl301ProtocolDecoder decoder = new Avl301ProtocolDecoder(new Avl301Protocol());

        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "244c0f086058500087335500010d0a"))));

        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "24480d1001c3065c0d00010d0a"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "24242c0f041710001d0e060146944904ff4ac40000148f0651044b001a081001be06590daa00000108a30d0a"))));

    }

}
