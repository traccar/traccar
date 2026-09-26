package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class GlobalSatProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeAlarmDismiss() throws Exception {

        var channel = channel(inject(new GlobalSatProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_DISMISS);

        verify(channel, command,
                text("GSC,123456789012345,Na*48!"));

    }

    @Test
    public void testEncodeOutputControl() throws Exception {

        var channel = channel(inject(new GlobalSatProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 1);
        command.set(Command.KEY_DATA, "1");

        verify(channel, command,
                text("GSC,123456789012345,Lo(1,1)*69!"));

    }

}
