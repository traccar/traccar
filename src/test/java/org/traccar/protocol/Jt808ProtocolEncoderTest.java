package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;
import org.traccar.model.Device;

import static org.mockito.Mockito.when;

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
            binary("7e820200060b3a73ce2ff20000000000000000d77e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_ALARM_DISMISS);
        verifyFrame(
            binary("7e820300060b3a73ce2ff20000000000000000d67e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_POWER_OFF);
        verifyFrame(
            binary("7e810500010b3a73ce2ff2000002d67e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_FACTORY_RESET);
        verifyFrame(
            binary("7e810500010b3a73ce2ff2000003d77e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "168.144.104.224");
        command.set(Command.KEY_PORT, 5015);
        verifyFrame(
            binary("7e8103001e0b3a73ce2ff2000002000000130f3136382e3134342e3130342e3232340000001804000013976a7e"),
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
            binary("7e810300130b3a73ce2ff2000002000000550400000064000000560400000023867e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 1);
        verifyFrame(
            binary("7e850000010b3a73ce2ff2000001d47e"),
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
            binary("7e82024006030000012345678901234500000000000000002b7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_ALARM_DISMISS);
        verifyFrame(
            binary("7e82034006030000012345678901234500000000000000002a7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_POWER_OFF);
        verifyFrame(
            binary("7e8105400103000001234567890123450000022a7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_FACTORY_RESET);
        verifyFrame(
            binary("7e8105400103000001234567890123450000032b7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "168.144.104.224");
        command.set(Command.KEY_PORT, 5015);
        verifyFrame(
            binary("7e8103401e0300000123456789012345000002000000130f3136382e3134342e3130342e323234000000180400001397967e"),
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
            binary("7e8103401303000001234567890123450000020000005504000000640000005604000000237a7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 1);
        verifyFrame(
            binary("7e850040010300000123456789012345000001287e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_CONFIGURATION);
        verifyFrame(
            binary("7e8104400003000001234567890123450000287e"),
            encodeCommand(encoder, decoder, command));

    }

    @Test
    public void testEncodeRealDevice() throws Exception {

        // Verified on a real BSJ KM02 device (uniqueId 024530313349, JT/T 808-2013) on 2026-08-29.
        // Every frame below is the exact byte sequence that was sent to the device over TCP and
        // acknowledged with a terminal general response (0x0001, result 0), except 0x8201 which is
        // answered directly with a location query response (0x0201) and needs no 0x8001 reply.

        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = inject(new Jt808ProtocolEncoder(null));
        var device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getUniqueId()).thenReturn("024530313349");

        Command command = new Command();
        command.setDeviceId(1);

        command.setType(Command.TYPE_POSITION_SINGLE);
        verifyFrame(
            binary("7e820100000245303133490000bf7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_SET_SPEED_LIMIT);
        command.set(Command.KEY_DATA, 100);
        verifyFrame(
            binary("7e81030013024530313349000002000000550400000064000000560400000023eb7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_MESSAGE);
        command.set(Command.KEY_MESSAGE, "Hello");
        verifyFrame(
            binary("7e8300000602453031334900000448656c6c6fff7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_VOICE_MESSAGE);
        verifyFrame(
            binary("7e8300000602453031334900000848656c6c6ff37e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_POSITION_STOP);
        verifyFrame(
            binary("7e820200060245303133490000000000000000ba7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 1);
        verifyFrame(
            binary("7e85000001024530313349000001b97e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_ALARM_DISMISS);
        verifyFrame(
            binary("7e820300060245303133490000000000000000bb7e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_REQUEST_PHOTO);
        command.set(Command.KEY_INDEX, 0); // real device test used default channel 0
        verifyFrame(
            binary("7e8801000c0245303133490000000001000000000000000000b87e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_CONFIGURATION);
        verifyFrame(
            binary("7e810400000245303133490000b97e"),
            encodeCommand(encoder, decoder, command));

        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "168.144.104.224");
        command.set(Command.KEY_PORT, 5015);
        verifyFrame(
            binary("7e8103001e024530313349000002000000130f3136382e3134342e3130342e323234000000180400001397077e"),
            encodeCommand(encoder, decoder, command));

    }

}
