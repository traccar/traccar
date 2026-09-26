package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class ItsProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var channel = channel(inject(new ItsProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verify(channel, command,
                text("@SET#RLP,OP1,"));

    }

}
