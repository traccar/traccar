package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;
import org.traccar.model.Device;

import static org.mockito.Mockito.when;

public class Minifinder2ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeNano() throws Exception {

        var encoder = inject(new Minifinder2ProtocolEncoder(null));

        var device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getModel()).thenReturn("Nano");

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_FIRMWARE_UPDATE);
        command.set(Command.KEY_DATA, "https://example.com");

        verifyCommand(encoder, command, binary("ab00160059d2010004143068747470733a2f2f6578616d706c652e636f6d"));

    }

}
