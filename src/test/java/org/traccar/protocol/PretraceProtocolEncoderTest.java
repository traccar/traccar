package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PretraceProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodePositionPeriodic() throws Exception {

        var encoder = inject(new PretraceProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 300);

        assertEquals("(123456789012345D221300,300,,^69)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeCustom() throws Exception {

        var encoder = inject(new PretraceProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "D21012");

        assertEquals("(123456789012345D21012^44)", encoder.encodeCommand(command));

    }

}
