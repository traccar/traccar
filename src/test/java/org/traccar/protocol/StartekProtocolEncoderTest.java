package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class StartekProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeEngineStop() throws Exception {

        var channel = channel(inject(new StartekProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verify(channel, command,
                text("$$:24,123456789012345,900,1,19F\r\n"));

    }

}
