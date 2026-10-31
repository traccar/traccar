package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class MeitrackProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var channel = channel(inject(new MeitrackProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);

        verifyEncode(channel, command,
                text("@@A25,123456789012345,A10*58\r\n"));

        command.setDeviceId(1);
        command.setType(Command.TYPE_REQUEST_PHOTO);

        verifyEncode(channel, command,
                text("@@A46,123456789012345,D03,1,camera_picture.jpg*1C\r\n"));

        command.setDeviceId(1);
        command.setType(Command.TYPE_SEND_SMS);
        command.set(Command.KEY_PHONE, "15360853789");
        command.set(Command.KEY_MESSAGE, "Meitrack");

        verifyEncode(channel, command,
                text("@@A48,123456789012345,C02,0,15360853789,Meitrack*8B\r\n"));

    }

}
