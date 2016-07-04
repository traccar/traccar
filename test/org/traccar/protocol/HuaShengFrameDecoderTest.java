package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class HuaShengFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        HuaShengFrameDecoder decoder = new HuaShengFrameDecoder();

        Assert.assertEquals(
                binary("c0010c00120060000000000004000600010100c0"),
                decoder.decode(null, null, binary("c0010c00120060000000000004000600010100c0")));

        Assert.assertEquals(
                binary("c0010c003e0002000000000010020012a0014f42445f3347315f56312e302e330013a0043335353835353035303434303635380006a08701000006a0a1035fc0"),
                decoder.decode(null, null, binary("c0010c003e0002000000000010020012a0014f42445f3347315f56312e302e330013a0043335353835353035303434303635380006a08701000006a0a1035fc0")));

    }

}
