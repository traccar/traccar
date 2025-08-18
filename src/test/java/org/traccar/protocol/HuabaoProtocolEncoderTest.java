package org.traccar.protocol;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class HuabaoProtocolEncoderTest extends ProtocolTest {

    @Disabled
    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new HuabaoProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyCommand(encoder, command, binary("7e81050001080201000027001ff0467e"));

    }

}
