package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class CityeasyProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        CityeasyProtocolEncoder encoder = new CityeasyProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_TIMEZONE);
        command.set(Command.KEY_TIMEZONE, 6 * 3600);

        verifyCommand(encoder, command, binary("5353001100080001680000000B60820D0A"));

    }

}
