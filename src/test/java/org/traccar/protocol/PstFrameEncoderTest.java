package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class PstFrameEncoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        PstFrameEncoder encoder = new PstFrameEncoder();

        ByteBuf result = Unpooled.buffer();
        encoder.encode(null, binary("2FAF0B10059A0000B001022FAF0B10E91349A2AD3B1DAD2FF8A78228A58F"), result);
        verifyFrame(binary("282FAF0B10059A0000B001022FAF0B10E91349A2AD3B1DAD2FF8A7822768A58F29"), result);
    }

}
