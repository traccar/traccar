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
    public void testEncodeCommands2013() throws Exception {

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
            binary("7e820200040b3a73ce2ff2000000000000d57e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_ALARM_DISMISS);
        verifyFrame(
            binary("7e820300040b3a73ce2ff2000000000000d47e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_POWER_OFF);
        verifyFrame(
            binary("7e810500010b3a73ce2ff2000002d67e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_FACTORY_RESET);
        verifyFrame(
            binary("7e810500010b3a73ce2ff2000003d77e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_REBOOT_DEVICE);
        verifyFrame(
            binary("7e810500010b3a73ce2ff2000004d07e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 60);
        verifyFrame(
            binary("7e8103000a0b3a73ce2ff200000100000028040000003cc87e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_ALARM_ARM);
        verifyFrame(
            binary("7e8103000b0b3a73ce2ff200000100000024050175736572e87e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_ALARM_DISARM);
        verifyFrame(
            binary("7e8103000b0b3a73ce2ff200000100000024050075736572e97e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "traccar.example.com");
        command.set(Command.KEY_PORT, 5015);
        verifyFrame(
            binary("7e810300220b3a73ce2ff20000020000001313747261636361722e"
                    + "6578616d706c652e636f6d000000180400001397167e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_MESSAGE);
        command.set(Command.KEY_MESSAGE, "Hello");
        verifyFrame(
            binary("7e830000060b3a73ce2ff200000448656c6c6f927e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_VOICE_MESSAGE);
        verifyFrame(
            binary("7e830000060b3a73ce2ff200000848656c6c6f9e7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_REQUEST_PHOTO);
        verifyFrame(
            binary("7e8801000c0b3a73ce2ff20000000001000000000000000000d57e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_SET_SPEED_LIMIT);
        command.set(Command.KEY_DATA, 100);
        verifyFrame(
            binary("7e810300080b3a73ce2ff200000100000046020064fa7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 1);
        verifyFrame(
            binary("7e850000020b3a73ce2ff200000101d67e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_CONFIGURATION);
        verifyFrame(
            binary("7e810400000b3a73ce2ff20000d47e"),
            encodeCommand(encoder, decoder, command));

    }

    @Test
    public void testEncodeCommands2019() throws Exception {

        var decoder = inject(new Jt808ProtocolDecoder(null));
        decoder.setProtocolVersion(3);
        var encoder = inject(new Jt808ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);

        command.setType(Command.TYPE_POSITION_SINGLE);
        verifyFrame(
            binary("7e82014000030000012345678901234500002e7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_POSITION_STOP);
        verifyFrame(
            binary("7e820240040300000123456789012345000000000000297e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_ALARM_DISMISS);
        verifyFrame(
            binary("7e820340040300000123456789012345000000000000287e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_POWER_OFF);
        verifyFrame(
            binary("7e8105400103000001234567890123450000022a7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_FACTORY_RESET);
        verifyFrame(
            binary("7e8105400103000001234567890123450000032b7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_REBOOT_DEVICE);
        verifyFrame(
            binary("7e8105400103000001234567890123450000042c7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 60);
        verifyFrame(
            binary("7e8103400a030000012345678901234500000100000028040000003c347e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_ALARM_ARM);
        verifyFrame(
            binary("7e8103400b030000012345678901234500000100000024050175736572147e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_ALARM_DISARM);
        verifyFrame(
            binary("7e8103400b030000012345678901234500000100000024050075736572157e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "traccar.example.com");
        command.set(Command.KEY_PORT, 5015);
        verifyFrame(
            binary("7e8103402203000001234567890123450000020000000113747261636361722e"
                    + "6578616d706c652e636f6d000000020400001397e27e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_MESSAGE);
        command.set(Command.KEY_MESSAGE, "Hello");
        verifyFrame(
            binary("7e83004006030000012345678901234500000448656c6c6f6e7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_VOICE_MESSAGE);
        verifyFrame(
            binary("7e83004006030000012345678901234500000848656c6c6f627e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_REQUEST_PHOTO);
        verifyFrame(
            binary("7e8801400c03000001234567890123450000000001000000000000000000297e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_SET_SPEED_LIMIT);
        command.set(Command.KEY_DATA, 100);
        verifyFrame(
            binary("7e81034008030000012345678901234500000100000046020064067e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 1);
        verifyFrame(
            binary("7e850040020300000123456789012345000001012a7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_CONFIGURATION);
        verifyFrame(
            binary("7e8104400003000001234567890123450000287e"),
            encodeCommand(encoder, decoder, command));

    }

}
