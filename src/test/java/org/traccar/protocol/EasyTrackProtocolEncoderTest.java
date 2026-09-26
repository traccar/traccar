package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class EasyTrackProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeEngineStop() throws Exception {

        var channel = channel(inject(new EasyTrackProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verify(channel, command,
                text("*ET,123456789012345,FD,Y1#"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "SPEED120");

        verify(channel, command,
                text("*ET,123456789012345,KS,SPEED120#"));

    }

}
