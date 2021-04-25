package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class UlbotechProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() {

        var encoder = new UlbotechProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "UNO;13912345678");

        verifyCommand(encoder, command, buffer("*TS01,UNO;13912345678#"));

    }

}
