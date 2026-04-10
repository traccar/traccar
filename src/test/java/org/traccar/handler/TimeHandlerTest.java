package org.traccar.handler;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeHandlerTest {

    @Test
    public void testAdjustRollover() {

        long currentTime = Instant.parse("2025-08-19T00:00:00Z").toEpochMilli();

        long invalidTime = currentTime - 1024 * Duration.ofDays(7).toMillis();
        long validTime = currentTime - Duration.ofDays(7).toMillis();

        assertEquals(currentTime, TimeHandler.adjustRollover(currentTime, new Date(invalidTime)).getTime());
        assertEquals(validTime, TimeHandler.adjustRollover(currentTime, new Date(validTime)).getTime());

    }

    @Test
    public void testInitialEpoch() {

        long currentTime = Instant.parse("2025-08-19T00:00:00Z").toEpochMilli();
        long epochTime = Instant.parse("1980-01-06T00:00:00Z").toEpochMilli();

        assertEquals(epochTime, TimeHandler.adjustRollover(currentTime, new Date(epochTime)).getTime());

    }

}
