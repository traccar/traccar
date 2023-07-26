package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GatorProtocolEncoderTest extends ProtocolTest {

    @Test
    void testEncodeId() throws Exception {
        var encoder = inject(new GatorProtocolEncoder(null));
        ByteBuf pseudoId = Unpooled.buffer();
        pseudoId.writeByte(0x20);
        pseudoId.writeByte(0x08);
        pseudoId.writeByte(0x95);
        pseudoId.writeByte(0x8C);
        assertEquals(pseudoId, encoder.encodeId(13332082112L));
    }

    @Test
    public void testEncode() throws Exception {
        var encoder = inject(new GatorProtocolEncoder(null));
        Command command = new Command();
        command.setDeviceId(13332082112L);
        command.setType(Command.TYPE_POSITION_SINGLE);
        verifyCommand(encoder, command, binary("24243000062008958C070D"));
    }
}
