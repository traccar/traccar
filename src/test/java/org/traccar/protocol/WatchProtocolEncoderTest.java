package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class WatchProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new WatchProtocolEncoder(null));

        Command command;

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);
        verifyFrame(buffer("[CS*123456789012345*0005*RESET]"), encoder.encodeCommand(null, command));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SOS_NUMBER);
        command.set(Command.KEY_INDEX, 1);
        command.set(Command.KEY_PHONE, "123456789");
        verifyFrame(buffer("[CS*123456789012345*000e*SOS1,123456789]"), encoder.encodeCommand(null, command));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_VOICE_MESSAGE);
        command.set(Command.KEY_DATA, "2321414d520a2573");
        verifyFrame(buffer("[CS*123456789012345*000b*TK,#!AMR\n%s]"), encoder.encodeCommand(null, command));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_VOICE_MESSAGE);
        command.set(Command.KEY_DATA, "7d5b5d2c2a");
        verifyFrame(concatenateBuffers(buffer("[CS*123456789012345*000d*TK,"), binary("7d017d027d037d047d05"), buffer("]")), encoder.encodeCommand(null, command));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_VOICE_MESSAGE);
        command.set(Command.KEY_DATA, "ff");
        verifyFrame(concatenateBuffers(buffer("[CS*123456789012345*0004*TK,"), binary("ff"), buffer("]")), encoder.encodeCommand(null, command));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_MESSAGE);
        command.set(Command.KEY_MESSAGE, "text");
        verifyFrame(buffer("[CS*123456789012345*0018*MESSAGE,0074006500780074]"), encoder.encodeCommand(null, command));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "WORK,6-9,11-13,13-15,17-19");
        verifyFrame(buffer("[CS*123456789012345*001a*WORK,6-9,11-13,13-15,17-19]"), encoder.encodeCommand(null, command));

    }

    @Test
    public void testEncodeTimezone() throws Exception {

        var encoder = inject(new WatchProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_TIMEZONE);

        command.set(Command.KEY_TIMEZONE, "Europe/Amsterdam");
        verifyFrame(buffer("[CS*123456789012345*0006*LZ,,+1]"), encoder.encodeCommand(null, command));

        command.set(Command.KEY_TIMEZONE, "GMT+01:30");
        verifyFrame(buffer("[CS*123456789012345*0008*LZ,,+1.5]"), encoder.encodeCommand(null, command));

        command.set(Command.KEY_TIMEZONE, "Atlantic/Azores");
        verifyFrame(buffer("[CS*123456789012345*0006*LZ,,-1]"), encoder.encodeCommand(null, command));

        command.set(Command.KEY_TIMEZONE, "GMT-11:30");
        verifyFrame(buffer("[CS*123456789012345*0009*LZ,,-11.5]"), encoder.encodeCommand(null, command));

        command.set(Command.KEY_LANGUAGE, 0);
        command.set(Command.KEY_TIMEZONE, "GMT+05:45");
        verifyFrame(buffer("[CS*123456789012345*000a*LZ,0,+5.75]"), encoder.encodeCommand(null, command));

    }

}
