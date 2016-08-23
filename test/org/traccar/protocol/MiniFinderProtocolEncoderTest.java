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
        command.set(Command.KEY_TIMEZONE, 3600);
        
        Assert.assertEquals("123456L+01", encoder.encodeCommand(command));

    }

}
