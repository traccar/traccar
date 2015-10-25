package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

import java.nio.ByteOrder;

public class WondexFrameDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Pt502FrameDecoder decoder = new Pt502FrameDecoder();

        Assert.assertNull(
                decoder.decode(null, null, binary("d0d70b0001ca9a3b")));

        Assert.assertEquals(
                binary("313034343938393630312c32303133303332333039353531352c31332e3537323737362c35322e3430303833382c302c3030302c37322c302c32"),
                decoder.decode(null, null, binary("313034343938393630312c32303133303332333039353531352c31332e3537323737362c35322e3430303833382c302c3030302c37322c302c320d0a")));

    }

}
