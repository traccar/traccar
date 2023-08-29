package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;
import org.traccar.model.Device;

import static org.mockito.Mockito.when;

public class GatorProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {
        var encoder = inject(new GatorProtocolEncoder(null));
        var device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getUniqueId()).thenReturn("13332082112");

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);
        verifyCommand(encoder, command, binary("24243000062008958C070D"));
    }

    @Test
    public void testEncodePeriodicPositionRetrievalIntervalSet() throws Exception {
        var encoder = inject(new GatorProtocolEncoder(null));
        var device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getUniqueId()).thenReturn("13088005658");

        Command command = new Command();
        command.setDeviceId(1);
        command.set(command.KEY_FREQUENCY, "5;5;120");
        command.setType(Command.TYPE_POSITION_PERIODIC);
        verifyCommand(encoder, command, binary("242434000b5800383a00050005781d0d"));
    }
}
