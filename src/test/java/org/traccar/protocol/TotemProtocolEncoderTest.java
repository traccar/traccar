package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class TotemProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var channel = channel(inject(new TotemProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(2);
        command.setType(Command.TYPE_REBOOT_DEVICE);
        command.set(Command.KEY_DEVICE_PASSWORD, "000000");

        verifyEncode(channel, command,
                text("$$0020CF000000,0061D"));

    }

    @Test
    public void testSmsEncode() throws Exception {

        var channel = channel(inject(new TotemProtocolSmsEncoder(null)));

        Command command = new Command();
        command.setDeviceId(2);
        command.setType(Command.TYPE_REBOOT_DEVICE);
        command.set(Command.KEY_DEVICE_PASSWORD, "000000");

        verifyEncode(channel, command,
                text("*000000,006#"));

    }

}
