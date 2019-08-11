package org.traccar.handler;

import org.junit.Before;
import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Position;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class FilterHandlerTest extends BaseTest {

    private FilterHandler passingHandler = new FilterHandler(new Config());
    private FilterHandler filteringHandler;

    @Before
    public void before() {
        Config config = new Config();
        config.setString(Keys.FILTER_INVALID, String.valueOf(true));
        config.setString(Keys.FILTER_ZERO, String.valueOf(true));
        config.setString(Keys.FILTER_DUPLICATE, String.valueOf(true));
        config.setString(Keys.FILTER_FUTURE, String.valueOf(5 * 60));
        config.setString(Keys.FILTER_APPROXIMATE, String.valueOf(true));
        config.setString(Keys.FILTER_STATIC, String.valueOf(true));
        config.setString(Keys.FILTER_DISTANCE, String.valueOf(10));
        config.setString(Keys.FILTER_MAX_SPEED, String.valueOf(500));
        config.setString(Keys.FILTER_SKIP_LIMIT, String.valueOf(10));
        config.setString(Keys.FILTER_SKIP_ATTRIBUTES_ENABLE, String.valueOf(true));
        filteringHandler = new FilterHandler(config);
    }

    private Position createPosition(
            long deviceId,
            Date time,
            boolean valid,
            double latitude,
            double longitude,
            double altitude,
            double speed,
            double course) {

        Position position = new Position();
        position.setDeviceId(deviceId);
        position.setTime(time);
        position.setValid(valid);
        position.setLatitude(latitude);
        position.setLongitude(longitude);
        position.setAltitude(altitude);
        position.setSpeed(speed);
        position.setCourse(course);
        return position;
    }

    @Test
    public void testFilter() {

        Position position = createPosition(0, new Date(), true, 10, 10, 10, 10, 10);

        assertNotNull(filteringHandler.handlePosition(position));
        assertNotNull(passingHandler.handlePosition(position));

        position = createPosition(0, new Date(Long.MAX_VALUE), true, 10, 10, 10, 10, 10);

        assertNull(filteringHandler.handlePosition(position));
        assertNotNull(passingHandler.handlePosition(position));

        position = createPosition(0, new Date(), false, 10, 10, 10, 10, 10);

        assertNull(filteringHandler.handlePosition(position));
        assertNotNull(passingHandler.handlePosition(position));

    }

    @Test
    public void testSkipAttributes() {

        Position position = createPosition(0, new Date(), true, 10, 10, 10, 0, 10);
        position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);

        assertNotNull(filteringHandler.handlePosition(position));

    }

}
