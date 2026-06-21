package org.traccar.handler;

import org.junit.jupiter.api.Test;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.handler.TimeHandler;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void testHybridTimeUsesDefaultSevenDaysThreshold() {
        Config config = new Config();
        CacheManager cacheManager = mock(CacheManager.class);
        Device device = new Device();
        device.setId(1);
        device.set(Keys.TIME_OVERRIDE.getKey(), "hybridTime");
        when(cacheManager.getObject(eq(Device.class), eq(1L))).thenReturn(device);
        when(cacheManager.getConfig()).thenReturn(config);

        TimeHandler handler = new TimeHandler(config, cacheManager);

        Date serverTime = Date.from(Instant.parse("2025-08-19T00:00:00Z"));
        Date deviceTime = Date.from(Instant.parse("2025-08-10T00:00:00Z"));
        Position position = new Position("test");
        position.setDeviceId(1);
        position.setServerTime(serverTime);
        position.setDeviceTime(deviceTime);
        position.setFixTime(deviceTime);

        handler.onPosition(position, ignored -> {
        });

        assertEquals(serverTime, position.getDeviceTime());
        assertEquals(serverTime, position.getFixTime());
    }

    @Test
    public void testHybridTimeKeepsDeviceTimeWithinThreshold() {
        Config config = new Config();
        CacheManager cacheManager = mock(CacheManager.class);
        Device device = new Device();
        device.setId(1);
        device.set(Keys.TIME_OVERRIDE.getKey(), "hybridTime");
        when(cacheManager.getObject(eq(Device.class), eq(1L))).thenReturn(device);
        when(cacheManager.getConfig()).thenReturn(config);

        TimeHandler handler = new TimeHandler(config, cacheManager);

        Date serverTime = Date.from(Instant.parse("2025-08-19T00:00:00Z"));
        Date deviceTime = Date.from(Instant.parse("2025-08-15T00:00:00Z"));
        Position position = new Position("test");
        position.setDeviceId(1);
        position.setServerTime(serverTime);
        position.setDeviceTime(deviceTime);
        position.setFixTime(deviceTime);

        handler.onPosition(position, ignored -> {
        });

        assertEquals(deviceTime, position.getDeviceTime());
        assertEquals(deviceTime, position.getFixTime());
    }

    @Test
    public void testHybridTimeUsesCustomThresholdFromConfig() {
        Config config = new Config();
        config.setString(Keys.HYBRID_TIME_DAYS, "3");
        CacheManager cacheManager = mock(CacheManager.class);
        Device device = new Device();
        device.setId(1);
        device.set(Keys.TIME_OVERRIDE.getKey(), "hybridTime");
        when(cacheManager.getObject(eq(Device.class), eq(1L))).thenReturn(device);
        when(cacheManager.getConfig()).thenReturn(config);

        TimeHandler handler = new TimeHandler(config, cacheManager);

        Date serverTime = Date.from(Instant.parse("2025-08-19T00:00:00Z"));
        Date deviceTime = Date.from(Instant.parse("2025-08-15T00:00:00Z"));
        Position position = new Position("test");
        position.setDeviceId(1);
        position.setServerTime(serverTime);
        position.setDeviceTime(deviceTime);
        position.setFixTime(deviceTime);

        handler.onPosition(position, ignored -> {
        });

        assertEquals(serverTime, position.getDeviceTime());
        assertEquals(serverTime, position.getFixTime());
    }

}
