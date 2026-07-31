package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class Itr120ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeEngineStop() throws Exception {
        var encoder = inject(new Itr120ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyCommand(encoder, command, binary("282880000f0001010000000152454c41592c3123"));
    }

    @Test
    public void testEncodeEngineResume() throws Exception {
        var encoder = inject(new Itr120ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_RESUME);

        verifyCommand(encoder, command, binary("282880000f0001010000000152454c41592c3023"));
    }

    @Test
    public void testEncodeCustom() throws Exception {
        var encoder = inject(new Itr120ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "TEST#");

        verifyCommand(encoder, command, binary("282880000c000101000000015445535423"));
    }

}
