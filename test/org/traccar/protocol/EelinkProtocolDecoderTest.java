package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

public class EelinkProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        EelinkProtocolDecoder decoder = new EelinkProtocolDecoder(new EelinkProtocol());

        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "676701000b001b035418804661834901"))));

        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "6767030004001A0001"))));

        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "6767070088001050E2281400FFFFFFFF02334455660333445566043344556605AA00000007334455660A334455660B334455660C4E2000000DAA0000000E334455660F3344556610AAAA000011334455661C334455661F334455662133445566423344556646334455664D334455665C334455665E33445566880000000089000000008A000000008B00000000"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "676702001b03c5538086df0190c1790b3482df0f0157020800013beb00342401"))));

    }

}
