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

        when(decoder.getHeader(1)).thenReturn(EelinkProtocolDecoder.HEADER_EELINK);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyEncode(channel, command,
                binary("676780000f0000010000000052454c41592c3123"));

    }

    @Test
    public void testEncodeUdp() throws Exception {

        var decoder = mock(EelinkProtocolDecoder.class);
        var encoder = inject(new EelinkProtocolEncoder(null, true));
        var channel = channel(decoder, encoder);

        when(decoder.getHeader(1)).thenReturn(EelinkProtocolDecoder.HEADER_EELINK);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyEncode(channel, command,
                binary("454c001eb41a0123456789012345676780000f0000010000000052454c41592c3123"));

    }

    @Test
    public void testEncodeIterTcp() throws Exception {

        var decoder = mock(EelinkProtocolDecoder.class);
        var encoder = inject(new EelinkProtocolEncoder(null, false));
        var channel = channel(decoder, encoder);

        when(decoder.getHeader(1)).thenReturn(EelinkProtocolDecoder.HEADER_ITER);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyEncode(channel, command,
                binary("282880000f0001010000000152454c41592c3123"));

    }

    @Test
    public void testEncodeIterUdp() throws Exception {

        var decoder = mock(EelinkProtocolDecoder.class);
        var encoder = inject(new EelinkProtocolEncoder(null, true));
        var channel = channel(decoder, encoder);

        when(decoder.getHeader(1)).thenReturn(EelinkProtocolDecoder.HEADER_ITER);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyEncode(channel, command,
                binary("454c001ed2260123456789012345282880000f0001010000000152454c41592c3123"));

    }

}
