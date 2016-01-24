package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class Gt06FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Gt06FrameDecoder decoder = new Gt06FrameDecoder();

        Assert.assertEquals(
                binary("78780d010352887071911998000479d00d0a"),
                decoder.decode(null, null, binary("78780d010352887071911998000479d00d0a")));

        Assert.assertEquals(
                binary("78782516000000000000c000000000000000000020000900fa0210ef00fb620006640301000468030d0a"),
                decoder.decode(null, null, binary("78782516000000000000c000000000000000000020000900fa0210ef00fb620006640301000468030d0a")));

    }

}
