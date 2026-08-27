package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class Jt808ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = inject(new Jt808ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);

        command.setType(Command.TYPE_ENGINE_STOP);

        verifyFrame(
            binary("7e810500010b3a73ce2ff20000f0247e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_CUSTOM);

        command.set(Command.KEY_DATA, "7e830000140b3a73ce2ff2000001546573742c20436f6d6d616e642c2031323323a57e");
        verifyFrame(
            binary("7e830000140b3a73ce2ff2000001546573742c20436f6d6d616e642c2031323323a57e"),
            encodeCommand(encoder, decoder, command));

        encoder.setModelOverride("BSJ");

        command.set(Command.KEY_DATA, "Test, Command, 123#");
        verifyFrame(
            binary("7e830000140b3a73ce2ff2000001546573742c20436f6d6d616e642c2031323323a57e"),
            encodeCommand(encoder, decoder, command));

    }

    @Test
    public void testEncodeJimiCustom() throws Exception {

        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = inject(new Jt808ProtocolEncoder(null));
        encoder.setModelOverride("JC371");

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "TEST");

        verifyFrame(
            binary("7e890000050b3a73ce2ff20000f0544553543b7e"),
            encodeCommand(encoder, decoder, command));

    }

}
