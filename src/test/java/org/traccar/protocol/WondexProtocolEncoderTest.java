package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WondexProtocolEncoderTest extends ProtocolTest {
    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new WondexProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(2);
        command.setType(Command.TYPE_POSITION_SINGLE);
        command.set(Command.KEY_DEVICE_PASSWORD, "0000");

        assertEquals("$WP+GETLOCATION=0000", encoder.encodeCommand(command));

    }

}
