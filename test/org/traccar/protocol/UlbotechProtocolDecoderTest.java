package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.TestDataManager;

import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;

public class UlbotechProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        UlbotechProtocolDecoder decoder = new UlbotechProtocolDecoder(new TestDataManager(), null, null);

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "F80101035785203457289495D60235010E016175A506C2C838000000000064"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "F8010108621060211481299C4247FA010E015EE1D606BDE797000301370081030402420000040400523CAF050603921743220706080000000000000000071131058E410C0E30310D48312F8E4131046A080402C8F2545445F8"))));
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "F8010108621060211481299C4249FA010E015EE27506BDE80900020000008F030402420000040400523CAF05060392173F220706080000000000000000071131058E410C0E40310D48312F8E41310884080402CA60E43872F8"))));
        
        //verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
        //        "F801010863070018987298729619022701CC0000285534BA4F28553AB14C2855BC9C572855BC9D5B28553AB25C2855445861285544586303040000000004040000076F0508033A18CF220D3DE6A638F8"))));

    }

}
