package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EelinkProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeTcp() throws Exception {

        var decoder = mock(EelinkProtocolDecoder.class);
        var encoder = inject(new EelinkProtocolEncoder(null, false));
        var channel = channel(decoder, encoder);

        when(decoder.getHeader()).thenReturn(EelinkProtocolDecoder.HEADER_EELINK);

        Command command;

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "version?");
        verifyEncode(channel, command, binary(
                "676780000f0000010000000076657273696f6e3f"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);
        verifyEncode(channel, command, binary(
                "676780000f0000010000000052454c41592c3123"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_RESUME);
        verifyEncode(channel, command, binary(
                "676780000f0000010000000052454c41592c3023"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);
        verifyEncode(channel, command, binary(
                "676780000d00000100000000524553455423"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);
        verifyEncode(channel, command, binary(
                "676780000d00000100000000574845524523"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_GET_VERSION);
        verifyEncode(channel, command, binary(
                "676780000f0000010000000056455253494f4e3f"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "ação");
        verifyEncode(channel, command, binary(
                "676780000d0000010000000061c3a7c3a36f"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CONFIGURATION);
        verifyEncode(channel, command, binary(
                "676780000d00000100000000504152414d3f"));

        when(decoder.getHeader()).thenReturn(EelinkProtocolDecoder.HEADER_ITR120);

        command = new Command();
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

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_GET_VERSION);
        verifyEncode(channel, command, binary(
                "282880000f0001010000000656455253494f4e3f"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "ação");
        verifyEncode(channel, command, binary(
                "282880000b00010100000007613f3f6f"));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CONFIGURATION);
        verifyEncode(channel, command, binary(
                "282880000d00010100000008504152414d3f"));

    }

    @Test
    public void testEncodeUdp() throws Exception {

        var channel = channel(inject(new EelinkProtocolEncoder(null, true)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyEncode(channel, command,
                binary("454c001eb41a0123456789012345676780000f0000010000000052454c41592c3123"));

    }

}
