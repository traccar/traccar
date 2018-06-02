package org.traccar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.traccar.model.Position;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class FilterHandlerTest {

    static {
        Context.init(new TestIdentityManager());
    }

    private FilterHandler filtingHandler;
    private FilterHandler passingHandler;

    @Before
    public void setUp() {
        passingHandler = new FilterHandler();
        filtingHandler = new FilterHandler();
        filtingHandler.setFilterInvalid(true);
        filtingHandler.setFilterZero(true);
        filtingHandler.setFilterDuplicate(true);
        filtingHandler.setFilterFuture(5 * 60);
        filtingHandler.setFilterApproximate(true);
        filtingHandler.setFilterStatic(true);
        filtingHandler.setFilterDistance(10);
        filtingHandler.setFilterMaxSpeed(500);
        filtingHandler.setSkipLimit(10);
    }

    @After
    public void tearDown() {
        filtingHandler = null;
        passingHandler = null;
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

        Position p = new Position();
        p.setDeviceId(deviceId);
        p.setTime(time);
        p.setValid(valid);
        p.setLatitude(latitude);
        p.setLongitude(longitude);
        p.setAltitude(altitude);
        p.setSpeed(speed);
        p.setCourse(course);
        return p;
    }

    @Test
    public void testFilterInvalid() throws Exception {

        Position position = createPosition(0, new Date(), true, 10, 10, 10, 10, 10);

        assertNotNull(filtingHandler.handlePosition(position));
        assertNotNull(passingHandler.handlePosition(position));

        position = createPosition(0, new Date(Long.MAX_VALUE), true, 10, 10, 10, 10, 10);

        assertNull(filtingHandler.handlePosition(position));
        assertNotNull(passingHandler.handlePosition(position));

        position = createPosition(0, new Date(), false, 10, 10, 10, 10, 10);

        assertNull(filtingHandler.handlePosition(position));
        assertNotNull(passingHandler.handlePosition(position));

        position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
        filtingHandler.setSkipAttributes(true);
        assertNotNull(filtingHandler.handlePosition(position));
    }

}
