package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class Jt600FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Jt600FrameDecoder decoder = new Jt600FrameDecoder();

        Assert.assertEquals(
                binary("28333132303832303032392C5730312C30323535332E333535352C452C323433382E303939372C532C412C3137313031322C3035333333392C302C382C32302C362C33312C352C32302C323029"),
                decoder.decode(null, null, binary("28333132303832303032392C5730312C30323535332E333535352C452C323433382E303939372C532C412C3137313031322C3035333333392C302C382C32302C362C33312C352C32302C323029")));

        Assert.assertEquals(
                binary("24312082002911001B171012053405243809970255335555000406140003EE2B91044D1F02"),
                decoder.decode(null, null, binary("24312082002911001B171012053405243809970255335555000406140003EE2B91044D1F02")));

    }

}
