package org.traccar.protocol;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class H02ProtocolEncoderTest extends ProtocolTest {

    H02ProtocolEncoder encoder = new H02ProtocolEncoder();
    DateTime dt = new DateTime().withHourOfDay(1).withMinuteOfHour(2).withSecondOfMinute(3);;

    @Test
    public void testAlarmArmEncode() throws Exception {

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_ARM);

        Assert.assertEquals("*HQ,123456789012345,SCF,010203,0,0#", encoder.encodeCommand(command, dt));
    }

    @Test
    public void testAlarmDisarmEncode() throws Exception {

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_DISARM);

        Assert.assertEquals("*HQ,123456789012345,SCF,010203,1,1#", encoder.encodeCommand(command, dt));
    }
}
