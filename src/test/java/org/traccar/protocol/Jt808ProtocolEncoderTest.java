package org.traccar.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.embedded.EmbeddedChannel;

import java.util.List;

public class Jt808ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeCustomTextMessageModels() throws Exception {

        var encoder = inject(new Jt808ProtocolEncoder(null));
        var decoder = inject(new Jt808ProtocolDecoder(null));
        var channel = new EmbeddedChannel(decoder);

        for (String model : List.of("BSJ", "C5", "C5L")) {

            encoder.setModelOverride(model);

            Command command = new Command();
            command.setDeviceId(1);
            command.setType(Command.TYPE_CUSTOM);
            command.set(Command.KEY_DATA, "Test, Command, 123#");

            ByteBuf encoded = (ByteBuf) encoder.encodeCommand(channel, command);

            assertEquals(0x8300, encoded.getUnsignedShort(1));
            assertEquals(0x0014, encoded.getUnsignedShort(3));
            assertTrue(ByteBufUtil.hexDump(encoded).contains(
                    "01546573742c20436f6d6d616e642c2031323323"));
        }
        
    }

    @Disabled
    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new Jt808ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyCommand(encoder, command, binary("7e81050001080201000027001ff0467e"));

    }

}
