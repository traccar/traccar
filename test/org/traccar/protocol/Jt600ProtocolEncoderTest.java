package org.traccar.protocol;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class Jt600ProtocolEncoderTest extends ProtocolTest {
    Jt600ProtocolEncoder encoder = new Jt600ProtocolEncoder();
    Command command = new Command();

    @Test
    public void testEngineStop() throws Exception {
        command.setType(Command.TYPE_ENGINE_STOP);
        assertEquals("(S07,0)", encoder.encodeCommand(command));
    }

    @Test
    public void testEngineResume() throws Exception {
        command.setType(Command.TYPE_ENGINE_RESUME);
        assertEquals("(S07,1)", encoder.encodeCommand(command));
    }

    @Test
    public void testSetTimezone() throws Exception {
        command.setType(Command.TYPE_SET_TIMEZONE);
        command.set(Command.KEY_TIMEZONE, 240 * 60);
        assertEquals("(S09,1,240)", encoder.encodeCommand(command));
    }

    @Test
    public void testReboot() throws Exception {
        command.setType(Command.TYPE_REBOOT_DEVICE);
        assertEquals("(S17)", encoder.encodeCommand(command));
    }
}
