package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class Gps056FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Gps056FrameDecoder decoder = new Gps056FrameDecoder();

        Assert.assertEquals(
                binary("242432354c4f474e5f3131383632343632303333373832393436322e3123"),
                decoder.decode(null, null, binary("242432354c4f474e5f3131383632343632303333373832393436322e3123")));

    }

}
