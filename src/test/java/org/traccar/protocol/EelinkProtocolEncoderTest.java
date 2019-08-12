package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class EelinkProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyCommand(new EelinkProtocolEncoder(null, false), command, binary("676780000f0000010000000052454c41592c3123"));

        verifyCommand(new EelinkProtocolEncoder(null, true), command, binary("454c001eb41a0123456789012345676780000f0000010000000052454c41592c3123"));

    }

}
