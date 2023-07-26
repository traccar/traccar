package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;
import org.traccar.model.Device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class GatorProtocolEncoderTest extends ProtocolTest {

    @Test
    void testEncodeId() throws Exception {
        var encoder = inject(new GatorProtocolEncoder(null));
        var device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getUniqueId()).thenReturn("13332082112");

        ByteBuf pseudoId = Unpooled.buffer();
        pseudoId.writeByte(0x20);
        pseudoId.writeByte(0x08);
        pseudoId.writeByte(0x95);
        pseudoId.writeByte(0x8C);
        assertEquals(pseudoId, encoder.encodeId(1));
    }

    @Test
    public void testEncode() throws Exception {
        var encoder = inject(new GatorProtocolEncoder(null));
        var device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getUniqueId()).thenReturn("13332082112");

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);
        verifyCommand(encoder, command, binary("24243000062008958C070D"));
    }
}
