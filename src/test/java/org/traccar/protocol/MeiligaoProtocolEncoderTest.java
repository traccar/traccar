package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class MeiligaoProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new MeiligaoProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);

        verifyCommand(encoder, command, binary("404000111234567890123441016cf70d0a"));

        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 100);

        verifyCommand(encoder, command, binary("40400013123456789012344102000a2f4f0d0a"));

        command.setType(Command.TYPE_SET_TIMEZONE);
        command.set(Command.KEY_TIMEZONE, "GMT+8");

        verifyCommand(encoder, command, binary("4040001412345678901234413234383030ad0d0a"));

        command.setType(Command.TYPE_REBOOT_DEVICE);

        verifyCommand(encoder, command, binary("40400011123456789012344902d53d0d0a"));

        command.setType(Command.TYPE_ALARM_GEOFENCE);
        command.set(Command.KEY_RADIUS, 1000);

        verifyCommand(encoder, command, binary("4040001312345678901234410603e87bb00d0a"));

    }

}
