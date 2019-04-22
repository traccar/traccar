package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class InmotionProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        InmotionProtocolEncoder encoder = new InmotionProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyCommand(encoder, command, binary("787811800b00000000554e41424c452300009df30d0a"));

        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_RESUME);

        verifyCommand(encoder, command, binary("787811800b00000000454e41424c4523000062670d0a"));

    }

}
