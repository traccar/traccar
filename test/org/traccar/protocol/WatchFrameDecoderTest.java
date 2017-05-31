package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class WatchFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        WatchFrameDecoder decoder = new WatchFrameDecoder();

        Assert.assertEquals(
                binary("5b33472a383330383430363237392a303030382a72636170747572655d"),
                decoder.decode(null, null, binary("5b33472a383330383430363237392a303030382a72636170747572655d")));

        Assert.assertEquals(
                binary("5b33472a383330383430363237392a303030392a4c4b2c302c302c38345d"),
                decoder.decode(null, null, binary("5b33472a383330383430363237392a303030392a4c4b2c302c302c38345d")));

    }

}
