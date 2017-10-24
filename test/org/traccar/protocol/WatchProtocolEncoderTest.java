package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class WatchProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        WatchProtocolEncoder encoder = new WatchProtocolEncoder();
        
        Command command;

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);
        Assert.assertEquals("[CS*123456789012345*0005*RESET]", encoder.encodeCommand(command));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SOS_NUMBER);
        command.set(Command.KEY_INDEX, 1);
        command.set(Command.KEY_PHONE, "123456789");
        Assert.assertEquals("[CS*123456789012345*000e*SOS1,123456789]", encoder.encodeCommand(command));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_VOICE_MESSAGE);
        command.set(Command.KEY_DATA, "3333");
        Assert.assertEquals("[CS*123456789012345*0005*TK,33]", encoder.encodeCommand(command));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "WORK,6-9,11-13,13-15,17-19");
        Assert.assertEquals("[CS*123456789012345*001a*WORK,6-9,11-13,13-15,17-19]", encoder.encodeCommand(command));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_TIMEZONE);
        command.set(Command.KEY_TIMEZONE, "Europe/Amsterdam");
        Assert.assertEquals("[CS*123456789012345*0006*LZ,,+1]", encoder.encodeCommand(command));

        command.set(Command.KEY_TIMEZONE, "GMT+01:30");
        Assert.assertEquals("[CS*123456789012345*0008*LZ,,+1.5]", encoder.encodeCommand(command));

        command.set(Command.KEY_TIMEZONE, "Atlantic/Azores");
        Assert.assertEquals("[CS*123456789012345*0006*LZ,,-1]", encoder.encodeCommand(command));

        command.set(Command.KEY_TIMEZONE, "GMT-11:30");
        Assert.assertEquals("[CS*123456789012345*0009*LZ,,-11.5]", encoder.encodeCommand(command));

    }

}
