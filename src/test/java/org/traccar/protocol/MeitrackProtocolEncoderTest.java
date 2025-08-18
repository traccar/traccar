package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MeitrackProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new MeitrackProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);

        assertEquals("@@A25,123456789012345,A10*58\r\n", encoder.encodeCommand(command));

        command.setDeviceId(1);
        command.setType(Command.TYPE_REQUEST_PHOTO);

        assertEquals("@@A46,123456789012345,D03,1,camera_picture.jpg*1C\r\n", encoder.encodeCommand(command));

        command.setDeviceId(1);
        command.setType(Command.TYPE_SEND_SMS);
        command.set(Command.KEY_PHONE, "15360853789");
        command.set(Command.KEY_MESSAGE, "Meitrack");

        assertEquals("@@A48,123456789012345,C02,0,15360853789,Meitrack*8B\r\n", encoder.encodeCommand(command));

    }

}
