package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class MiniFinderProtocolEncoderTest extends ProtocolTest {

    private String prefix = "123456";
    private MiniFinderProtocolEncoder encoder;

    @Before
    public void setup() {
        encoder = new MiniFinderProtocolEncoder();
    }

    @Test
    public void testEncodeCustom() throws Exception {
        String expected = String.format("%sM,700", prefix);
        Command command = new Command();
        command.setType(Command.CUSTOM);
        command.set(expected, 1);
        Object encoded = encoder.encodeCommand(command);
        assert expected.equals(encoded);
    }

    @Test
    public void testEncodeUnsupportedCommand() throws Exception {
        Command command = new Command();
        command.setType("UNSUPPORTED");
        Object o = encoder.encodeCommand(command);
        assert o == null;
    }

}
