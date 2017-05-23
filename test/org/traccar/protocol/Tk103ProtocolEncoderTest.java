package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class Tk103ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeEngineStop() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        Assert.assertEquals("(123456789012345AV011)", encoder.encodeCommand(command));

    }

}
