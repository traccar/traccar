package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class JmakProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var channel = channel(inject(new JmakProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyEncode(channel, command, text("$34;1;#"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 2);
        command.set(Command.KEY_DATA, "1");

        verifyEncode(channel, command, text("$94;1;#"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "jmak.com.br");
        command.set(Command.KEY_PORT, 5259);

        verifyEncode(channel, command, text("$43;jmak.com.br;|12;5259;#"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "$29;532.6;|30;122.2;#");

        verifyEncode(channel, command, text("$29;532.6;|30;122.2;#"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_SPEED_LIMIT);
        command.set(Command.KEY_DATA, 80);

        verifyEncode(channel, command, text("$16;1;|17;80;#"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_REMOVE);
        command.set(Command.KEY_ENABLE, true);

        verifyEncode(channel, command, text("$55;1;#"));

    }

    @Test
    public void testEncodeSms() throws Exception {

        var channel = channel(inject(new JmakProtocolSmsEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        verifyEncode(channel, command, text("JMAK#REBOOT#"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 3);
        command.set(Command.KEY_DATA, "2");

        verifyEncode(channel, command, text("JMAK#s;167;2#"));

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
