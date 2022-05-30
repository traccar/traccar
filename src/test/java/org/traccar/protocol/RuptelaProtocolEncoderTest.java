package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class RuptelaProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new RuptelaProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);

        command.set(Command.KEY_DATA, " Setio 2,1");
        verifyCommand(encoder, command, binary("000b6c20536574696F20322C31eb3e"));

        command.set(Command.KEY_DATA, "000b890100000000007fffffff89f0");
        verifyCommand(encoder, command, binary("000b890100000000007fffffff89f0"));

    }

}
