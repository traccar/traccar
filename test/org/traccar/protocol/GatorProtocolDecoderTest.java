package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolDecoderTest;
import org.traccar.helper.ChannelBufferTools;

public class GatorProtocolDecoderTest extends ProtocolDecoderTest {
    
    @Test
    public void testDecodeId() {
        
        Assert.assertEquals("3512345006", GatorProtocolDecoder.decodeId(12, 162, 50, 134));
        
    }

    @Test
    public void testDecode() throws Exception {

        GatorProtocolDecoder decoder = new GatorProtocolDecoder(new GatorProtocol());

        verifyNothing(decoder, binary(
                "242421000643e30282070d"));

        verifyPosition(decoder, binary(
                "24248000260009632d141121072702059226180104367500000000c04700079c0c34000ad80b00ff000a0d"),
                position("2014-11-21 07:27:02.000", true, 59.37697, 10.72792));

        verifyPosition(decoder, binary(
                "24248100230CA23285100306145907022346901135294700000000C04001012C0E1100000021CB0D"));

        verifyPosition(decoder, binary(
                "2424800023c2631e00111220104909833268648703804100000000c0470000000b4e00000000550d"),
                position("2011-12-20 10:49:09.000", true, -33.44773, -70.63402));

        verifyPosition(decoder, binary(
                "24248000260009632d141121072702059226180104367500000000c04700079c0c34000ad80b00ff000a0d"));
        
    }

}
