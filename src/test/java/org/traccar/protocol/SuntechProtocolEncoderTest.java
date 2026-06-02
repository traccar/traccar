package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SuntechProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var decoder = inject(new SuntechProtocolDecoder(null));
        var encoder = inject(new SuntechProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        assertEquals(
            "SA200CMD;123456789012345;02;Reboot\r",
            encodeCommand(encoder, decoder, command));

    }

}
