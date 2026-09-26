package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;

public class MobilogixProtocolEncoderTest extends ProtocolTest {

    private final Date time = Date.from(
            LocalDateTime.of(LocalDate.of(2025, 2, 22), LocalTime.of(1, 2, 3)).atZone(ZoneOffset.systemDefault()).toInstant());

    @Test
    public void testEncode() throws Exception {

        var channel = channel(inject(new MobilogixProtocolEncoder(null, time)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyEncode(channel, command,
                text("[2025-02-22 01:02:03,S6,RELAY=1]"));
    }

}
