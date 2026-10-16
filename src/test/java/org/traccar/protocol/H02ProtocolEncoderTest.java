package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;

public class H02ProtocolEncoderTest extends ProtocolTest {

    private final Date time = Date.from(
            LocalDateTime.of(LocalDate.now(), LocalTime.of(1, 2, 3)).atZone(ZoneOffset.systemDefault()).toInstant());

    @Test
    public void testAlarmArmEncode() throws Exception {

        var channel = channel(inject(new H02ProtocolEncoder(null, time)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_ARM);

        verifyEncode(channel, command,
                text("*HQ,123456789012345,SCF,010203,0,0#"));
    }

    @Test
    public void testAlarmDisarmEncode() throws Exception {

        var channel = channel(inject(new H02ProtocolEncoder(null, time)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_DISARM);

        verifyEncode(channel, command,
                text("*HQ,123456789012345,SCF,010203,1,1#"));
    }

    @Test
    public void testEngineStopEncode() throws Exception {

        var channel = channel(inject(new H02ProtocolEncoder(null, time)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyEncode(channel, command,
                text("*HQ,123456789012345,S20,010203,1,1#"));
    }

    @Test
    public void testEngineResumeEncode() throws Exception {

        var channel = channel(inject(new H02ProtocolEncoder(null, time)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_RESUME);

        verifyEncode(channel, command,
                text("*HQ,123456789012345,S20,010203,1,0#"));
    }

    @Test
    public void testPositionPeriodicEncode() throws Exception {

        var channel = channel(inject(new H02ProtocolEncoder(null, time)));

        Command command = new Command();
        command.setDeviceId(1);
        command.set(Command.KEY_FREQUENCY, 10);
        command.setType(Command.TYPE_POSITION_PERIODIC);

        verifyEncode(channel, command,
                text("*HQ,123456789012345,S71,010203,22,10#"));
    }

}
