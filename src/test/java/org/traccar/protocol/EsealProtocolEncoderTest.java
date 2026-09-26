package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class EsealProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var channel = channel(inject(new EsealProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_DISARM);

        verify(channel, command,
                text("##S,eSeal,123456789012345,256,3.0.8,RC-Unlock,E##"));

    }

}
