package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

public class H02FrameDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        H02FrameDecoder decoder = new H02FrameDecoder();

        Assert.assertEquals(
                binary("2a48512c3335333538383036303031353536382c56312c3139333530352c412c3830392e303031302c532c333435342e383939372c572c302e30302c302e30302c3239313031332c65666666666266662c3030303264342c3030303030622c3030353338352c3030353261612c323523"),
                decoder.decode(null, null, binary("2a48512c3335333538383036303031353536382c56312c3139333530352c412c3830392e303031302c532c333435342e383939372c572c302e30302c302e30302c3239313031332c65666666666266662c3030303264342c3030303030622c3030353338352c3030353261612c323523")));

        Assert.assertEquals(
                binary("24430025645511183817091319355128000465632432000100ffe7fbffff0000"),
                decoder.decode(null, null, binary("24430025645511183817091319355128000465632432000100ffe7fbffff0000")));

    }

}
