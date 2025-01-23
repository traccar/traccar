package org.digitalegiz.protocol;

import org.junit.jupiter.api.Test;
import org.digitalegiz.ProtocolTest;
import org.digitalegiz.model.Command;
import org.digitalegiz.model.Device;

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
}
