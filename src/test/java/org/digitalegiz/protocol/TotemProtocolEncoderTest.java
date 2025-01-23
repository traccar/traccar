package org.digitalegiz.protocol;

import org.junit.jupiter.api.Test;
import org.digitalegiz.ProtocolTest;
import org.digitalegiz.model.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TotemProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new TotemProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(2);
        command.setType(Command.TYPE_REBOOT_DEVICE);
        command.set(Command.KEY_DEVICE_PASSWORD, "000000");

        assertEquals("$$0020CF000000,0061D", encoder.encodeCommand(command));

    }
    
    @Test
    public void testSmsEncode() throws Exception {

        var encoder = inject(new TotemProtocolSmsEncoder(null));

        Command command = new Command();
        command.setDeviceId(2);
        command.setType(Command.TYPE_REBOOT_DEVICE);
        command.set(Command.KEY_DEVICE_PASSWORD, "000000");

        assertEquals("*000000,006#", encoder.encodeCommand(command));

    }

}
