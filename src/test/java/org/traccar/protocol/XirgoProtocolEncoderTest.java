package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.Assert.assertEquals;

public class XirgoProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new XirgoProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 0);
        command.set(Command.KEY_DATA, 1);

        assertEquals("+XT:7005,2,1", encoder.encodeCommand(command));

    }

}
