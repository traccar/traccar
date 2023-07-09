package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Gps103ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodePositionPeriodic() throws Exception {

        var encoder = inject(new Gps103ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 300);

        assertEquals("**,imei:123456789012345,C,05m", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeCustom() throws Exception {

        var encoder = inject(new Gps103ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "H,080");

        assertEquals("**,imei:123456789012345,H,080", encoder.encodeCommand(command));

    }

}
