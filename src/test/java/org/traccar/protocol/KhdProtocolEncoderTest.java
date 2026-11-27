package org.traccar.protocol;

import io.netty.buffer.ByteBufUtil;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KhdProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var channel = channel(inject(new KhdProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);

        command.setType(Command.TYPE_ENGINE_STOP);
        verifyEncode(channel, command,
                binary("29293900065981972d5d0d"));

        command.setType(Command.TYPE_POSITION_SINGLE);
        verifyEncode(channel, command,
                binary("29293000065981972d540d"));

        command.setType(Command.TYPE_REBOOT_DEVICE);
        verifyEncode(channel, command,
                binary("29293200065981972d560d"));

        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "Hello!");
        verifyEncode(channel, command,
                binary("29293a000c5981972d48656c6c6f21370d"));

        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 15);
        verifyEncode(channel, command,
                binary("29297b000a5981972d000fffff1c0d"));

    }

    @Test
    public void testEncodeIdentifier() {
        var encoder = new KhdProtocolEncoder(null);

        assertEquals("c8dca318", ByteBufUtil.hexDump(encoder.encodeIdentifier("14472923524")));
        assertEquals("1dd08262", ByteBufUtil.hexDump(encoder.encodeIdentifier("29800298")));
    }

}
