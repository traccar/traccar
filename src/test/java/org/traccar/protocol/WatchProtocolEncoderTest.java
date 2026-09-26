package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class WatchProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var channel = channel(inject(new WatchProtocolEncoder(null)));

        Command command;

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);
        verify(channel, command,
                buffer("[CS*123456789012345*0005*RESET]"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SOS_NUMBER);
        command.set(Command.KEY_INDEX, 1);
        command.set(Command.KEY_PHONE, "123456789");
        verify(channel, command,
                buffer("[CS*123456789012345*000e*SOS1,123456789]"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_VOICE_MESSAGE);
        command.set(Command.KEY_DATA, "2321414d520a2573");
        verify(channel, command,
                buffer("[CS*123456789012345*000b*TK,#!AMR\n%s]"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_VOICE_MESSAGE);
        command.set(Command.KEY_DATA, "7d5b5d2c2a");
        verify(channel, command,
                concatenateBuffers(buffer("[CS*123456789012345*000d*TK,"), binary("7d017d027d037d047d05"), buffer("]")));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_VOICE_MESSAGE);
        command.set(Command.KEY_DATA, "ff");
        verify(channel, command,
                concatenateBuffers(buffer("[CS*123456789012345*0004*TK,"), binary("ff"), buffer("]")));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_MESSAGE);
        command.set(Command.KEY_MESSAGE, "text");
        verify(channel, command,
                buffer("[CS*123456789012345*0018*MESSAGE,0074006500780074]"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "WORK,6-9,11-13,13-15,17-19");
        verify(channel, command,
                buffer("[CS*123456789012345*001a*WORK,6-9,11-13,13-15,17-19]"));

    }

    @Test
    public void testEncodeTimezone() throws Exception {

        var channel = channel(inject(new WatchProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_TIMEZONE);

        command.set(Command.KEY_TIMEZONE, "Europe/Amsterdam");
        verify(channel, command,
                buffer("[CS*123456789012345*0006*LZ,,+1]"));

        command.set(Command.KEY_TIMEZONE, "GMT+01:30");
        verify(channel, command,
                buffer("[CS*123456789012345*0008*LZ,,+1.5]"));

        command.set(Command.KEY_TIMEZONE, "Atlantic/Azores");
        verify(channel, command,
                buffer("[CS*123456789012345*0006*LZ,,-1]"));

        command.set(Command.KEY_TIMEZONE, "GMT-11:30");
        verify(channel, command,
                buffer("[CS*123456789012345*0009*LZ,,-11.5]"));

        command.set(Command.KEY_LANGUAGE, 0);
        command.set(Command.KEY_TIMEZONE, "GMT+05:45");
        verify(channel, command,
                buffer("[CS*123456789012345*000a*LZ,0,+5.75]"));

    }

}
