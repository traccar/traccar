package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.Assert.assertEquals;

public class StrongTowerProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodePositionPeriodic() throws Exception {

        StrongTowerProtocolEncoder encoder = new StrongTowerProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 300);

        assertEquals("{\"id\":1,\"cmd\":\"" + Command.TYPE_POSITION_PERIODIC + "\",\"" + Command.KEY_FREQUENCY + "\":300}", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeCustom() throws Exception {

        StrongTowerProtocolEncoder encoder = new StrongTowerProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "hello");

        assertEquals("{\"data\":\"hello\",\"id\":1,\"cmd\":\"" + Command.TYPE_CUSTOM + "\"}", encoder.encodeCommand(command));

    }

}
