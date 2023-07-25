package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GatorProtocolEncoderTest extends ProtocolTest {

    @Test
    void encodeId() throws Exception {
        var encoder = inject(new GatorProtocolEncoder(null));
        assertEquals("2008958C", encoder.encodeId(13332082112L));
    }

    @Test
    public void testEncode() throws Exception {
        var encoder = inject(new GatorProtocolEncoder(null));
        Command command = new Command();
        command.setDeviceId(13332082112L);
        command.setType(Command.TYPE_POSITION_SINGLE);
        verifyCommand(encoder, command, binary("24243000062008958C070D"));
    }
}
