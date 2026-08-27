package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Jt808FrameEncoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var encoder = new Jt808FrameEncoder();

        ByteBuf buf = Unpooled.buffer();
        encoder.encode(null, binary("7e307e087d557e"), buf);
        verifyFrame(binary("7e307d02087d01557e"), buf);

    }

}
