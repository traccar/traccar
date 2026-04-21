package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class Xexun2ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new Xexun2ProtocolEncoder(null));

        Command command;

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POWER_OFF);
        verifyCommand(encoder, command, binary("FAAF0007000112345678901234500004FEBC6F663D31FAAF"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 150);
        verifyCommand(encoder, command, binary("FAAF0007000112345678901234500015F90E747261636B696E675F73656E643D3135302C313530FAAF"));

    }

}
