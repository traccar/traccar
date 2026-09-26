package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class MiniFinderProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var channel = channel(inject(new MiniFinderProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_TIMEZONE);
        command.set(Command.KEY_TIMEZONE, "GMT+1");

        verify(channel, command,
                text("123456L+01"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SOS_NUMBER);
        command.set(Command.KEY_INDEX, 2);
        command.set(Command.KEY_PHONE, "1111111111");

        verify(channel, command,
                text("123456C1,1111111111"));

    }

}
