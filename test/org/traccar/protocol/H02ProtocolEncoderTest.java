package org.traccar.protocol;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class H02ProtocolEncoderTest extends ProtocolTest {

    private H02ProtocolEncoder encoder = new H02ProtocolEncoder();
    private DateTime time = new DateTime().withHourOfDay(1).withMinuteOfHour(2).withSecondOfMinute(3);;

    @Test
    public void testAlarmArmEncode() throws Exception {

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_ARM);

        Assert.assertEquals("*HQ,123456789012345,SCF,010203,0,0#", encoder.encodeCommand(command, time));
    }

    @Test
    public void testAlarmDisarmEncode() throws Exception {

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_DISARM);

        Assert.assertEquals("*HQ,123456789012345,SCF,010203,1,1#", encoder.encodeCommand(command, time));
    }

    @Test
    public void testEngineStopEncode() throws Exception {

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        Assert.assertEquals("*HQ,123456789012345,S20,010203,1,3,10,3,5,5,3,5,3,5,3,5#", encoder.encodeCommand(command, time));
    }

    @Test
    public void testEngineResumeEncode() throws Exception {

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_RESUME);

        Assert.assertEquals("*HQ,123456789012345,S20,010203,0,0#", encoder.encodeCommand(command, time));
    }

    @Test
    public void testPositionPeriodicEncode() throws Exception {

        Command command = new Command();
        command.setDeviceId(1);
        command.set(Command.KEY_FREQUENCY, 10);
        command.setType(Command.TYPE_POSITION_PERIODIC);

        Assert.assertEquals("*HQ,123456789012345,S71,010203,22,10#", encoder.encodeCommand(command, time));
    }

}
