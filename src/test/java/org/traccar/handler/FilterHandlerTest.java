package org.traccar.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traccar.BaseTest;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.util.Date;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FilterHandlerTest extends BaseTest {

    private FilterHandler passingHandler;
    private FilterHandler filteringHandler;

    @BeforeEach
    public void passingHandler() {
        var device = mock(Device.class);
        when(device.getAttributes()).thenReturn(new HashMap<>());
        var config = mock(Config.class);
        var cacheManager = mock(CacheManager.class);
        when(cacheManager.getConfig()).thenReturn(config);
        when(cacheManager.getObject(any(), anyLong())).thenReturn(device);
        passingHandler = new FilterHandler(cacheManager, null);
    }

    @BeforeEach
    public void filteringHandler() {
        var device = mock(Device.class);
        when(device.getAttributes()).thenReturn(new HashMap<>());
        var config = mock(Config.class);
        when(config.getString(Keys.FILTER_INVALID.getKey())).thenReturn("true");
        when(config.getString(Keys.FILTER_ZERO.getKey())).thenReturn("true");
        when(config.getString(Keys.FILTER_DUPLICATE.getKey())).thenReturn("true");
        when(config.getString(Keys.FILTER_APPROXIMATE.getKey())).thenReturn("true");
        when(config.getString(Keys.FILTER_STATIC.getKey())).thenReturn("true");
        when(config.getString(Keys.FILTER_DISTANCE.getKey())).thenReturn("10");
        when(config.getString(Keys.FILTER_MAX_SPEED.getKey())).thenReturn("500");
        when(config.getString(Keys.FILTER_SKIP_LIMIT.getKey())).thenReturn("10");
        when(config.getString(Keys.FILTER_SKIP_ATTRIBUTES_ENABLE.getKey())).thenReturn("true");
        when(config.getString(Keys.FILTER_SKIP_ATTRIBUTES.getKey())).thenReturn("alarm,result");
        var cacheManager = mock(CacheManager.class);
        when(cacheManager.getConfig()).thenReturn(config);
        when(cacheManager.getObject(any(), anyLong())).thenReturn(device);
        filteringHandler = new FilterHandler(cacheManager, null);
    }

    private Position createPosition(Date time, boolean valid, double speed) {
        Position position = new Position();
        position.setDeviceId(0);
        position.setTime(time);
        position.setValid(valid);
        position.setLatitude(10);
        position.setLongitude(10);
        position.setAltitude(10);
        position.setSpeed(speed);
        position.setCourse(10);
        return position;
    }

    @Test
    public void testFilter() {

        Position position = createPosition(new Date(), true, 10);

        assertFalse(filteringHandler.filter(position));
        assertFalse(passingHandler.filter(position));

        position = createPosition(new Date(Long.MAX_VALUE), true, 10);

        assertTrue(filteringHandler.filter(position));
        assertTrue(passingHandler.filter(position));

        position = createPosition(new Date(), false, 10);

        assertTrue(filteringHandler.filter(position));
        assertFalse(passingHandler.filter(position));

    }

    @Test
    public void testSkipAttributes() {

        Position position = createPosition(new Date(), true, 0);
        position.addAlarm(Position.ALARM_GENERAL);

        assertFalse(filteringHandler.filter(position));

    }

}
