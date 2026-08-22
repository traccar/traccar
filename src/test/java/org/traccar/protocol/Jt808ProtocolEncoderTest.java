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

    @Test
    public void testEncodeSetConnection() throws Exception {

        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = inject(new Jt808ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "1.2.3.4");
        command.set(Command.KEY_PORT, 5555);

        verifyFrame(
            binary("7e810300160b3a73ce2ff20000020000001307312e322e332e340000001804000015b3437e"),
            encodeCommand(encoder, decoder, command));

    }

    @Test
    public void testEncodeOtherCommands() throws Exception {

        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = inject(new Jt808ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);

        command.setType(Command.TYPE_POSITION_SINGLE);
        verifyFrame(
            binary("7e820100000b3a73ce2ff20000d27e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_POSITION_STOP);
        verifyFrame(
            binary("7e820200060b3a73ce2ff20000000000000000d77e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_ALARM_DISMISS);
        verifyFrame(
            binary("7e820300020b3a73ce2ff200000000d27e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_POWER_OFF);
        verifyFrame(
            binary("7e810500010b3a73ce2ff2000002d67e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_FACTORY_RESET);
        verifyFrame(
            binary("7e810500010b3a73ce2ff2000003d77e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_MESSAGE);
        command.set(Command.KEY_MESSAGE, "Test");
        verifyFrame(
            binary("7e830000050b3a73ce2ff200000454657374e57e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_VOICE_MESSAGE);
        verifyFrame(
            binary("7e830000050b3a73ce2ff200000854657374e97e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_REQUEST_PHOTO);
        verifyFrame(
            binary("7e8801000b0b3a73ce2ff200000001000000000000000000d27e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_SET_SPEED_LIMIT);
        command.set(Command.KEY_DATA, 80);
        verifyFrame(
            binary("7e810300080b3a73ce2ff200000100000055020050dd7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 2);
        verifyFrame(
            binary("7e850000010b3a73ce2ff2000002d77e"),
            encodeCommand(encoder, decoder, command));

    }

}
