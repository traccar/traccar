package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class Pt502ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        Pt502ProtocolEncoder encoder = new Pt502ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 2);
        command.set(Command.KEY_DATA, 1);
        
        Assert.assertEquals("000000OPC2,1", encoder.encodeCommand(command));

    }

}
