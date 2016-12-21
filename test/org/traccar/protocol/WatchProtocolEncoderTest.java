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
        command.setType(Command.TYPE_SET_TIMEZONE);
        command.set(Command.KEY_TIMEZONE, 60 * 60);
        Assert.assertEquals("[CS*123456789012345*0006*LZ,,+1]", encoder.encodeCommand(command));

        command.set(Command.KEY_TIMEZONE, 90 * 60);
        Assert.assertEquals("[CS*123456789012345*0008*LZ,,+1.5]", encoder.encodeCommand(command));

        command.set(Command.KEY_TIMEZONE, -60 * 60);
        Assert.assertEquals("[CS*123456789012345*0006*LZ,,-1]", encoder.encodeCommand(command));

        command.set(Command.KEY_TIMEZONE, -11 * 60 * 60 - 30 * 60);
        Assert.assertEquals("[CS*123456789012345*0009*LZ,,-11.5]", encoder.encodeCommand(command));
    }

}
