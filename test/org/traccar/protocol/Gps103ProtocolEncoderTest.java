package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.model.Command;

public class Gps103ProtocolEncoderTest {

    @Test
    public void testDecode() throws Exception {

        Gps103ProtocolEncoder encoder = new Gps103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 300);
        
        Assert.assertEquals("**,imei:123456789012345,C,05m;", encoder.encodeCommand(command));

    }

}
