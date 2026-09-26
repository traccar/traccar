package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class Jt808ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = inject(new Jt808ProtocolEncoder(null));
        var channel = channel(decoder, encoder);

        Command command = new Command();
        command.setDeviceId(1);

        command.setType(Command.TYPE_ENGINE_STOP);
        verify(channel, command,
                binary("7e810500010b3a73ce2ff20000f0247e"));

        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "7e830000140b3a73ce2ff2000001546573742c20436f6d6d616e642c2031323323a57e");
        verify(channel, command,
                binary("7e830000140b3a73ce2ff2000001546573742c20436f6d6d616e642c2031323323a57e"));

        encoder.setModelOverride("BSJ");

        command.set(Command.KEY_DATA, "Test, Command, 123#");
        verify(channel, command,
                binary("7e830000140b3a73ce2ff2000001546573742c20436f6d6d616e642c2031323323a57e"));

        command.setType(Command.TYPE_POSITION_SINGLE);
        verify(channel, command,
                binary("7e820100000b3a73ce2ff20000d27e"));

        command.setType(Command.TYPE_POSITION_STOP);
        verify(channel, command,
                binary("7e820200060b3a73ce2ff20000000000000000d77e"));

        command.setType(Command.TYPE_ALARM_DISMISS);
        verify(channel, command,
                binary("7e820300060b3a73ce2ff20000000000000000d67e"));

        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "168.144.104.224");
        command.set(Command.KEY_PORT, 5015);
        verify(channel, command,
                binary("7e8103001e0b3a73ce2ff2000002000000130f3136382e3134342e3130342e3232340000001804000013976a7e"));

        command.setType(Command.TYPE_MESSAGE);
        command.set(Command.KEY_MESSAGE, "Hello");
        verify(channel, command,
                binary("7e830000060b3a73ce2ff200000448656c6c6f927e"));

        command.setType(Command.TYPE_REQUEST_PHOTO);
        verify(channel, command,
                binary("7e8801000c0b3a73ce2ff20000000001000000000000000000d57e"));

        command.setType(Command.TYPE_SET_SPEED_LIMIT);
        command.set(Command.KEY_DATA, 100);
        verify(channel, command,
                binary("7e8103000a0b3a73ce2ff2000001000000550400000064ed7e"));

        command.set(Command.KEY_DURATION, 35);
        verify(channel, command,
                binary("7e810300130b3a73ce2ff2000002000000550400000064000000560400000023867e"));

        command.setType(Command.TYPE_CONFIGURATION);
        verify(channel, command,
                binary("7e810400000b3a73ce2ff20000d47e"));

        decoder.setProtocolVersion(3);

        command.setType(Command.TYPE_POSITION_SINGLE);
        verify(channel, command,
                binary("7e82014000030000012345678901234500002e7e"));

    }

    @Test
    public void testEncodeJimiCustom() throws Exception {

        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = inject(new Jt808ProtocolEncoder(null));
        var channel = channel(decoder, encoder);
        encoder.setModelOverride("JC371");

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "TEST");

        verify(channel, command,
                binary("7e890000050b3a73ce2ff20000f0544553543b7e"));

    }

}
