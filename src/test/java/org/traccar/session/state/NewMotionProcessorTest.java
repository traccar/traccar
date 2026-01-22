package org.traccar.session.state;

import org.junit.jupiter.api.Test;
import org.traccar.BaseTest;
import org.traccar.helper.DistanceCalculator;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.reports.common.TripsConfig;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NewMotionProcessorTest extends BaseTest {

    private static Position position(String time, double latitude, double longitude) throws ParseException {
        Position position = new Position();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        position.setTime(dateFormat.parse(time));
        position.setLatitude(latitude);
        position.setLongitude(longitude);
        position.setDeviceId(1);
        return position;
    }

    @Test
    public void testMotionDetected() throws ParseException {
        TripsConfig tripsConfig = new TripsConfig(500, 300000, 0, 0, false, false);

        double latitude = 0.0;
        double delta600 = DistanceCalculator.getLongitudeDelta(600, latitude);

        Deque<Position> positions = new ArrayDeque<>();
        positions.add(position("2017-01-01 00:00:00", latitude, 0.0));
        positions.add(position("2017-01-01 00:02:00", latitude, 0.0));
        positions.add(position("2017-01-01 00:04:00", latitude, 0.0));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);

        Position current = position("2017-01-01 00:05:00", latitude, delta600);
        NewMotionProcessor.updateState(state, current, tripsConfig);

        assertTrue(state.getMotionStreak());
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvent().getType());
        assertEquals(current.getFixTime(), state.getEvent().getEventTime());
    }

    @Test
    public void testStopDetected() throws ParseException {
        TripsConfig tripsConfig = new TripsConfig(500, 300000, 0, 0, false, false);

        double latitude = 0.0;
        double delta100 = DistanceCalculator.getLongitudeDelta(100, latitude);

        Deque<Position> positions = new ArrayDeque<>();
        positions.add(position("2017-01-01 00:00:00", latitude, 0.0));
        positions.add(position("2017-01-01 00:02:00", latitude, delta100));
        positions.add(position("2017-01-01 00:04:00", latitude, delta100));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);

        Position current = position("2017-01-01 00:06:00", latitude, delta100);
        NewMotionProcessor.updateState(state, current, tripsConfig);

        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvent().getType());
        assertEquals(current.getFixTime(), state.getEvent().getEventTime());
        assertFalse(state.getMotionStreak());
    }

    @Test
    public void testNoStopBeforeDuration() throws ParseException {
        TripsConfig tripsConfig = new TripsConfig(500, 300000, 0, 0, false, false);

        double latitude = 0.0;
        double delta100 = DistanceCalculator.getLongitudeDelta(100, latitude);

        Deque<Position> positions = new ArrayDeque<>();
        positions.add(position("2017-01-01 00:00:00", latitude, 0.0));
        positions.add(position("2017-01-01 00:01:00", latitude, delta100));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);

        Position current = position("2017-01-01 00:02:00", latitude, delta100);
        NewMotionProcessor.updateState(state, current, tripsConfig);

        assertNull(state.getEvent());
        assertTrue(state.getMotionStreak());
    }

    @Test
    public void testNoEventWhenAlreadyMoving() throws ParseException {
        TripsConfig tripsConfig = new TripsConfig(500, 300000, 0, 0, false, false);

        double latitude = 0.0;
        double delta600 = DistanceCalculator.getLongitudeDelta(600, latitude);

        Deque<Position> positions = new ArrayDeque<>();
        positions.add(position("2017-01-01 00:00:00", latitude, 0.0));
        positions.add(position("2017-01-01 00:02:00", latitude, 0.0));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);

        Position current = position("2017-01-01 00:03:00", latitude, delta600);
        NewMotionProcessor.updateState(state, current, tripsConfig);

        assertNull(state.getEvent());
        assertTrue(state.getMotionStreak());
    }

}
