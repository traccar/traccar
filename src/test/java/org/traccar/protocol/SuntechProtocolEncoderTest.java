package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class SuntechProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var decoder = inject(new SuntechProtocolDecoder(null));
        var encoder = inject(new SuntechProtocolEncoder(null));
        var channel = channel(decoder, encoder);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        verifyEncode(channel, command,
                text("SA200CMD;123456789012345;02;Reboot\r"));

    }

}
