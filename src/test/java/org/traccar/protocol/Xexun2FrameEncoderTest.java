package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Xexun2FrameEncoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Xexun2FrameEncoder encoder = new Xexun2FrameEncoder();

        ByteBuf result = Unpooled.buffer();
        encoder.encode(null, binary("FAAF123456FAAF123456FBBF123456FAAF"), result);
        verifyFrame(binary("FAAF123456FBBF01123456FBBF02123456FAAF"), result);

    }

}
