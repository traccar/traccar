package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class EelinkProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeTcp() throws Exception {

        var decoder = inject(new EelinkProtocolDecoder(null));
        var channel = channel(inject(new EelinkProtocolEncoder(null, false)), decoder);

        verifyDecode(decoder, binary("676701000a00010869137082327123"));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyEncode(channel, command,
                binary("676780000f0000010000000052454c41592c3123"));

    }

    @Test
    public void testEncodeItr120() throws Exception {

        var decoder = inject(new EelinkProtocolDecoder(null));
        var channel = channel(inject(new EelinkProtocolEncoder(null, false)), decoder);

        verifyDecode(decoder, binary("282801000a00010869137082327123"));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyEncode(channel, command,
                binary("282880000f0000010000000052454c41592c3123"));

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
