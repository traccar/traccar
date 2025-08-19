package org.traccar.handler;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeHandlerTest {

    @Test
    public void testAdjustRollover() {

        long currentTime = 1755577777; // Aug 19, 2025

        long invalidTime = currentTime - 1024 * Duration.ofDays(7).toMillis();
        long validTime = currentTime - Duration.ofDays(7).toMillis();

        assertEquals(currentTime, TimeHandler.adjustRollover(currentTime, new Date(invalidTime)).getTime());
        assertEquals(validTime, TimeHandler.adjustRollover(currentTime, new Date(validTime)).getTime());

    }

}
