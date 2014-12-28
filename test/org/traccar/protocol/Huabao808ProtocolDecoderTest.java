package org.traccar.protocol;


import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.TestDataManager;

import static org.junit.Assert.assertNull;

import static org.traccar.helper.DecoderVerifier.verify;

public class Huabao808ProtocolDecoderTest {

    @Test
    public void testPlatformRegistry() throws Exception {

        /*
        Huabao808ProtocolDecoder decoder = new Huabao808ProtocolDecoder(new TestDataManager(), null, null);

        assertNull (decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertArray(
                new int[]{}))));
        */
    }

    @Test
    public void testLocation() throws Exception {

        /*
        Huabao808ProtocolDecoder decoder = new Huabao808ProtocolDecoder(new TestDataManager(), null, null);

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertArray(
                new int[]{}))));
        */
    }

}
