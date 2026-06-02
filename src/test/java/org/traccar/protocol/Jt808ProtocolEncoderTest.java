package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class Jt808ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = inject(new Jt808ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyFrame(
            binary("7e810500010b3a73ce2ff20000f0247e"),
            encodeCommand(encoder, decoder, command));

    }

}
