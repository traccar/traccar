package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class GatorProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeEngineStart() throws Exception {

        var encoder = inject(new GatorProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_RESUME);

        verifyCommand(encoder, command, binary("2424380006000000003e0d"));
    }

    @Test
    public void testEncodeEngineStop() throws Exception {

        var encoder = inject(new GatorProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyCommand(encoder, command, binary("2424390006000000003f0d"));

    }

    @Test
    public void testEncodeGetPosition() throws Exception {

        var encoder = inject(new GatorProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);

        verifyCommand(encoder, command, binary("242430000600000000360d"));

    }

}