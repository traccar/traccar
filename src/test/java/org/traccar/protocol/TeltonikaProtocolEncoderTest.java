package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class TeltonikaProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new TeltonikaProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);

        command.set(Command.KEY_DATA, "setdigout 11");
        verifyCommand(encoder, command, binary("00000000000000140c01050000000c7365746469676f7574203131010000bed5"));

        command.set(Command.KEY_DATA, "03030000000185E8");
        verifyCommand(encoder, command, binary("00000000000000100c01050000000803030000000185e8010000da8b"));

    }

}
