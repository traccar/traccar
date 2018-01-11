package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class MiniFinderProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        MiniFinderProtocolEncoder encoder = new MiniFinderProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_TIMEZONE);
        command.set(Command.KEY_TIMEZONE, "GMT+1");

        Assert.assertEquals("123456L+01", encoder.encodeCommand(command));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SOS_NUMBER);
        command.set(Command.KEY_INDEX, 2);
        command.set(Command.KEY_PHONE, "1111111111");

        Assert.assertEquals("123456C1,1111111111", encoder.encodeCommand(command));

    }

}
