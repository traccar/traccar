package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;
import org.traccar.model.Device;

import static org.mockito.Mockito.when;

public class Xexun3ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new Xexun3ProtocolEncoder(null));
        var channel = channel(encoder);
        var device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getUniqueId()).thenReturn("862596080926201");

        Command command;

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "tk=2,20,0,600,1");
        verifyEncode(channel, command,
                binary("fc001a0321010862596080926201746b3d322c32302c302c3630302c31b0fbcf"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);
        verifyEncode(channel, command,
                binary("fc000f032101086259608092620172743d31e1bfcf"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 150);
        verifyEncode(channel, command,
                binary("fc001b0321010862596080926201746b3d322c3135302c302c3135302c31a661cf"));
    }

}
