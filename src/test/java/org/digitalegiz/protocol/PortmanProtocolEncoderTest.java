package org.digitalegiz.protocol;

import org.junit.jupiter.api.Test;
import org.digitalegiz.ProtocolTest;
import org.digitalegiz.model.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PortmanProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeEngineStop() throws Exception {

        var encoder = inject(new PortmanProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        assertEquals("&&123456789012345,XA5\r\n", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeEngineResume() throws Exception {

        var encoder = inject(new PortmanProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_RESUME);

        assertEquals("&&123456789012345,XA6\r\n", encoder.encodeCommand(command));

    }

}
