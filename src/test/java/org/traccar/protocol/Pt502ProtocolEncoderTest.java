package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class Pt502ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeCustom() throws Exception {

        var channel = channel(inject(new Pt502ProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "#PTI300");

        verifyEncode(channel, command,
                text("#PTI300\r\n"));

    }

    @Test
    public void testEncodeOutputControl() throws Exception {

        var channel = channel(inject(new Pt502ProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 2);
        command.set(Command.KEY_DATA, "1");

        verifyEncode(channel, command,
                text("#OPC2,1\r\n"));

    }

    @Test
    public void testEncodeTimezone() throws Exception {

        var channel = channel(inject(new Pt502ProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_TIMEZONE);
        command.set(Command.KEY_TIMEZONE, "GMT+8");

        verifyEncode(channel, command,
                text("#TMZ8\r\n"));

    }


    @Test
    public void testEncodeAlarmSpeed() throws Exception {

        var channel = channel(inject(new Pt502ProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_SPEED);
        command.set(Command.KEY_DATA, 120);

        verifyEncode(channel, command,
                text("#SPD120\r\n"));

    }

}
