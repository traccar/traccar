package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class EelinkProtocolEncoderTest extends ProtocolTest {

    private Command command(String type) {
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(type);
        return command;
    }

    private EelinkProtocolEncoder itr120Encoder() throws Exception {
        var encoder = inject(new EelinkProtocolEncoder(null, false));
        encoder.setModelOverride("iTR120");
        return encoder;
    }

    @Test
    public void testEncodeTcp() throws Exception {

        var encoder = inject(new EelinkProtocolEncoder(null, false));

        verifyCommand(encoder, command(Command.TYPE_ENGINE_STOP),
                binary("676780000f0000010000000052454c41592c3123"));

    }

    @Test
    public void testEncodeUdp() throws Exception {

        var encoder = inject(new EelinkProtocolEncoder(null, true));

        verifyCommand(encoder, command(Command.TYPE_ENGINE_STOP),
                binary("454c001eb41a0123456789012345676780000f0000010000000052454c41592c3123"));

    }

    @Test
    public void testEncodeEngineStopItr120() throws Exception {

        var encoder = itr120Encoder();

        verifyCommand(encoder, command(Command.TYPE_ENGINE_STOP),
                binary("282880000f0001010000000152454c41592c3123"));

    }

    @Test
    public void testEncodeEngineResumeItr120() throws Exception {

        var encoder = itr120Encoder();

        verifyCommand(encoder, command(Command.TYPE_ENGINE_RESUME),
                binary("282880000f0001010000000152454c41592c3023"));

    }

    @Test
    public void testEncodeCustomItr120() throws Exception {

        var encoder = itr120Encoder();

        Command command = command(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "TEST#");

        verifyCommand(encoder, command, binary("282880000c000101000000015445535423"));

    }

}
