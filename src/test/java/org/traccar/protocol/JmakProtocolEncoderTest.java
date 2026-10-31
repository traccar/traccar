package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class JmakProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var channel = channel(inject(new JmakProtocolEncoder(null, false)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyEncode(channel, command, text("$34;1;#"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "jmak.com.br");
        command.set(Command.KEY_PORT, 5259);

        verifyEncode(channel, command, text("$43;jmak.com.br;|12;5259;#"));

    }

    @Test
    public void testEncodeSms() throws Exception {

        var channel = channel(inject(new JmakProtocolEncoder(null, true)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        verifyEncode(channel, command, text("JMAK#REBOOT#"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "191.233.246.121");
        command.set(Command.KEY_PORT, 5259);

        verifyEncode(channel, command, text("JMAK#s;13;191.233.246.121#s;12;5259#"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_BATTERY);
        command.set(Command.KEY_ENABLE, false);

        verifyEncode(channel, command, text("JMAK#e;57;0#"));

    }

}
