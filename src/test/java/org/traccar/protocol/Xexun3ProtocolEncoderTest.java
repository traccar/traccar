package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class Xexun3ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var channel = channel(inject(new Xexun3ProtocolEncoder(null)));

        Command command;

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "tk=2,20,0,600,1");
        verifyEncode(channel, command,
                binary("fc001a0321010123456789012345746b3d322c32302c302c3630302c31e4ffcf"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 150);
        verifyEncode(channel, command,
                binary("fc001b0321010123456789012345746b3d322c3135302c302c3135302c31b810cf"));
    }

}
