package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class IterProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var channel = channel(inject(new IterProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "version?");
        verifyEncode(channel, command, binary(
                "282880000f0001010000000176657273696f6e3f"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);
        verifyEncode(channel, command, binary(
                "282880000f0001010000000252454c41592c3123"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_RESUME);
        verifyEncode(channel, command, binary(
                "282880000f0001010000000352454c41592c3023"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);
        verifyEncode(channel, command, binary(
                "282880000d00010100000004524553455423"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);
        verifyEncode(channel, command, binary(
                "282880000e00010100000005504147494e4723"));

    }

}
