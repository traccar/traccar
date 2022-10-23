package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.Assert.assertEquals;

public class Pt502ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeCustom() throws Exception {

        var encoder = inject(new Pt502ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "#PTI300");

        assertEquals("#PTI300\r\n", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeOutputControl() throws Exception {

        var encoder = inject(new Pt502ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 2);
        command.set(Command.KEY_DATA, "1");

        assertEquals("#OPC2,1\r\n", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeTimezone() throws Exception {

        var encoder = inject(new Pt502ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_TIMEZONE);
        command.set(Command.KEY_TIMEZONE, "GMT+8");

        assertEquals("#TMZ8\r\n", encoder.encodeCommand(command));

    }


    @Test
    public void testEncodeAlarmSpeed() throws Exception {

        var encoder = inject(new Pt502ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_SPEED);
        command.set(Command.KEY_DATA, 120);

        assertEquals("#SPD120\r\n", encoder.encodeCommand(command));

    }

}
