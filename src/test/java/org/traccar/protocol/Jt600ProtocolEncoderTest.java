package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class Jt600ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEngineStop() throws Exception {

        var channel = channel(inject(new Jt600ProtocolEncoder(null)));

        Command command = new Command();
        command.setType(Command.TYPE_ENGINE_STOP);

        verify(channel, command,
                text("(S07,0)"));

    }

    @Test
    public void testEngineResume() throws Exception {

        var channel = channel(inject(new Jt600ProtocolEncoder(null)));

        Command command = new Command();
        command.setType(Command.TYPE_ENGINE_RESUME);

        verify(channel, command,
                text("(S07,1)"));

    }

    @Test
    public void testSetTimezone() throws Exception {

        var channel = channel(inject(new Jt600ProtocolEncoder(null)));

        Command command = new Command();
        command.setType(Command.TYPE_SET_TIMEZONE);
        command.set(Command.KEY_TIMEZONE, "GMT+4");

        verify(channel, command,
                text("(S09,1,240)"));

    }

    @Test
    public void testReboot() throws Exception {

        var channel = channel(inject(new Jt600ProtocolEncoder(null)));

        Command command = new Command();
        command.setType(Command.TYPE_REBOOT_DEVICE);

        verify(channel, command,
                text("(S17)"));

    }

}
