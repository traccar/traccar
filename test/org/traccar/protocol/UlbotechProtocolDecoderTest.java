package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import static org.traccar.helper.DecoderVerifier.verify;

public class UlbotechProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        UlbotechProtocolDecoder decoder = new UlbotechProtocolDecoder(new UlbotechProtocol());

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "f8010103596580419465449da89d16010efe5580fe0923d82100140129005903040242000004040001a7f10506037818be220e070e31057b410c1324310d144131fa3208040020b1418297f8"))));
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "f8010103596580419465449da8564e010efe55a1800923d04b0000000000710304000000000404000178d2050603571876220ec3caf8"))));
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "f8010103545500500179009ccb4b62010e00144db906310d3f0000000000cb0304000000000404000a8123050603211860221006080000000100000000ef97f8"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "F80101035785203457289495D60235010E016175A506C2C838000000000064"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "F8010108621060211481299C4247FA010E015EE1D606BDE797000301370081030402420000040400523CAF050603921743220706080000000000000000071131058E410C0E30310D48312F8E4131046A080402C8F2545445F8"))));
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "F8010108621060211481299C4249FA010E015EE27506BDE80900020000008F030402420000040400523CAF05060392173F220706080000000000000000071131058E410C0E40310D48312F8E41310884080402CA60E43872F8"))));

        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "f8010108653280262660481cdacf830209ffffffffffffffff780304000300000404000000030506017418a021f99697f8"))));

        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "f801010865328026266048fffeae800209ffffffffffffffff7803040200000004040000000005060375175421f3060800000000000000009c28f8"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "f8010108653280262660489ce260b4010e01e757bd022340d7002b010d01570304020200000404000000260506036a17d42200060800000000000000000a0101ab9ff8"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "f8010108653280262660489ce260df010e01e756f30223384a0003010a02a80304020200000404000001280506036217fe22010608000000000000000005aaf8"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "f8010108653280262660489ce26128010e01e769ac022336290014010300730304020200000404000003c905060371181c2201060800000000000000000a0140e471f8"))));

    }

}
