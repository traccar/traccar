package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class PstProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeEngineStop() {

        var encoder = new PstProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyCommand(encoder, command, binary("860ddf790600000001060002ffffffffe42b"));

    }

    @Test
    public void testEncodeEngineResume() {

        var encoder = new PstProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_RESUME);

        verifyCommand(encoder, command, binary("860ddf790600000001060001ffffffff0af9"));

    }

}
