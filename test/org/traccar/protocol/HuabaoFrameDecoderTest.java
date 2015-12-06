package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class HuabaoFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        HuabaoFrameDecoder decoder = new HuabaoFrameDecoder();

        Assert.assertEquals(
                binary("7e307e087d557e"),
                decoder.decode(null, null, binary("7e307d02087d01557e")));

    }

}
