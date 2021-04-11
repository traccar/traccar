package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class TeltonikaProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var encoder = new TeltonikaProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);

        command.set(Command.KEY_DATA, "setdigout 11");
        verifyCommand(encoder, command, binary("00000000000000160C01050000000E7365746469676F75742031310D0A010000E258"));

        command.set(Command.KEY_DATA, "03030000000185E8");
        verifyCommand(encoder, command, binary("00000000000000100c01050000000803030000000185e8010000da8b"));

    }

}
