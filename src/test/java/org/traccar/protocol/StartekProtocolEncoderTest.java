package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.Assert.assertEquals;

public class StartekProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeEngineStop() {

        var encoder = new StartekProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        assertEquals("$$:23,123456789012345,900,1,19E\r\n", encoder.encodeCommand(null, command));

    }

}
