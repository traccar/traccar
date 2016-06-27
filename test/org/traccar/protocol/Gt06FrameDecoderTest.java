package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class Gt06FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Gt06FrameDecoder decoder = new Gt06FrameDecoder();

        Assert.assertEquals(
                binary("78781f1210020e140613cc04770690003e3f2e3414b20000000000000000044c446a0d0a"),
                decoder.decode(null, null, binary("78781f1210020e140613cc04770690003e3f2e3414b20000000000000000044c446a0d0a")));

        Assert.assertEquals(
                binary("787808134606020002044dc5050d0a"),
                decoder.decode(null, null, binary("787808134606020002044dc5050d0a")));

        Assert.assertEquals(
                binary("78781f1210020e14061dcc0476fcd0003e3faf3e14b20000000000000000044ef6740d0a"),
                decoder.decode(null, null, binary("78781f1210020e14061dcc0476fcd0003e3faf3e14b20000000000000000044ef6740d0a")));

        Assert.assertEquals(
                binary("78780d010352887071911998000479d00d0a"),
                decoder.decode(null, null, binary("78780d010352887071911998000479d00d0a")));

        Assert.assertEquals(
                binary("78782516000000000000c000000000000000000020000900fa0210ef00fb620006640301000468030d0a"),
                decoder.decode(null, null, binary("78782516000000000000c000000000000000000020000900fa0210ef00fb620006640301000468030d0a")));

    }

}
