package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class WondexProtocolEncoderTest extends ProtocolTest {
    @Test
    public void testEncode() throws Exception {

        var channel = channel(inject(new WondexProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(2);
        command.setType(Command.TYPE_POSITION_SINGLE);
        command.set(Command.KEY_DEVICE_PASSWORD, "0000");

        verifyEncode(channel, command,
                text("$WP+GETLOCATION=0000"));

    }

}
