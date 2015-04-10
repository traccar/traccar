package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.TestDataManager;

import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;

public class CalAmpProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        CalAmpProtocolDecoder decoder = new CalAmpProtocolDecoder(new TestDataManager(), null, null);
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "8308353301059723580f01020102088952d994c352d994c4134fa767c4c482e20000c12700000d29006e1002019affc90f061d00060c0000")), null));
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "8308355233050116134f01020102445652d993e152d993e1124c728cc68f0647000023c00000000000000e02019affc90f071c0015020000")), null));
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "830545420185450101010200075517fb335516c5c40fb1aea4cf4cbf250000000000000000008900260015ffb10f001108110a0000")), null));
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "830543321494750101010A00085492798A0EC4F9E71BDA3B81005600040F1F33050000030000000076000000000000000000000000")), null));

    }

}
