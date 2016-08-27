package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class MeitrackProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        MeitrackProtocolEncoder encoder = new MeitrackProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);

        Assert.assertEquals("@@Q25,123456789012345,A10*68\r\n", encoder.encodeCommand(command));

        command.setDeviceId(1);
        command.setType(Command.TYPE_SEND_SMS);
        command.set(Command.KEY_PHONE, "15360853789");
        command.set(Command.KEY_MESSAGE, "Meitrack");

        Assert.assertEquals("@@f48,123456789012345,C02,0,15360853789,Meitrack*B0\r\n", encoder.encodeCommand(command));

    }

}
