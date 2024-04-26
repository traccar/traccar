package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;


public class ZrProtocolEncoderTest extends ProtocolTest {
    @Test
    public void testEncodePositionPeriodic() throws Exception {

        var encoder = inject(new ZrProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_PORT, 5257);
        command.set(Command.KEY_SERVER, "127.0.0.1");

        verifyCommand(encoder, command, binary("dddd0034104000000001234567890123450700110001202391000312345624c000150302011484093132372e302e302e3100000000012c12ffff"));
    }
}
