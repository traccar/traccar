package org.traccar.session.state;

import org.junit.jupiter.api.Test;
import org.traccar.BaseTest;
import org.traccar.helper.DistanceCalculator;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        double minDistance = 500;
        long minDuration = 300000;

        double latitude = 0.0;
        double delta600 = DistanceCalculator.getLongitudeDelta(600, latitude);

        Deque<Position> positions = new ArrayDeque<>();
        positions.add(position("2017-01-01 00:00:00", latitude, 0.0));
        positions.add(position("2017-01-01 00:02:00", latitude, 0.0));
        positions.add(position("2017-01-01 00:04:00", latitude, 0.0));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);

        Position current = position("2017-01-01 00:05:00", latitude, delta600);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration);

        assertTrue(state.getMotionStreak());
        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvents().get(0).getType());
        assertEquals(current.getFixTime(), state.getEvents().get(0).getEventTime());
        assertEquals(current.getFixTime(), state.getEventTime());
        assertEquals(current.getLatitude(), state.getEventLatitude());
        assertEquals(current.getLongitude(), state.getEventLongitude());
    }

    @Test
    public void testStopDetected() throws ParseException {
        double minDistance = 500;
        long minDuration = 300000;

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
        NewMotionProcessor.updateState(state, current, minDistance, minDuration);

        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvents().get(0).getType());
        assertEquals(current.getFixTime(), state.getEvents().get(0).getEventTime());
        assertFalse(state.getMotionStreak());
        assertEquals(current.getFixTime(), state.getEventTime());
        assertEquals(current.getLatitude(), state.getEventLatitude());
        assertEquals(current.getLongitude(), state.getEventLongitude());
    }

    @Test
    public void testNoStopBeforeDuration() throws ParseException {
        double minDistance = 500;
        long minDuration = 300000;

        double latitude = 0.0;
        double delta100 = DistanceCalculator.getLongitudeDelta(100, latitude);

        Deque<Position> positions = new ArrayDeque<>();
        positions.add(position("2017-01-01 00:00:00", latitude, 0.0));
        positions.add(position("2017-01-01 00:01:00", latitude, delta100));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);

        Position current = position("2017-01-01 00:02:00", latitude, delta100);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration);

        assertTrue(state.getEvents().isEmpty());
        assertTrue(state.getMotionStreak());
    }

    @Test
    public void testNoEventWhenAlreadyMoving() throws ParseException {
        double minDistance = 500;
        long minDuration = 300000;

        double latitude = 0.0;
        double delta600 = DistanceCalculator.getLongitudeDelta(600, latitude);

        Deque<Position> positions = new ArrayDeque<>();
        positions.add(position("2017-01-01 00:00:00", latitude, 0.0));
        positions.add(position("2017-01-01 00:02:00", latitude, 0.0));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);

        Position current = position("2017-01-01 00:03:00", latitude, delta600);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration);

        assertTrue(state.getEvents().isEmpty());
        assertTrue(state.getMotionStreak());
    }

    @Test
    public void testMotionWithFastAverageSpeed() throws ParseException {
        double minDistance = 500;
        long minDuration = 300000;

        double latitude = 0.0;
        double delta1200 = DistanceCalculator.getLongitudeDelta(1200, latitude);

        Deque<Position> positions = new ArrayDeque<>();
        positions.add(position("2017-01-01 00:00:00", latitude, 0.0));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);

        Position current = position("2017-01-01 00:10:00", latitude, delta1200);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration);

        assertTrue(state.getMotionStreak());
        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvents().get(0).getType());
        assertEquals(current.getFixTime(), state.getEvents().get(0).getEventTime());
    }

    @Test
    public void testSlowSpeedStopMotionStop() throws ParseException {
        double minDistance = 500;
        long minDuration = 300000;

        double latitude = 0.0;
        double delta700 = DistanceCalculator.getLongitudeDelta(700, latitude);

        Deque<Position> positions = new ArrayDeque<>();
        Position last = position("2017-01-01 00:00:00", latitude, 0.0);
        positions.add(last);

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);

        Position current = position("2017-01-01 00:10:00", latitude, delta700);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration);

        assertFalse(state.getMotionStreak());
        assertEquals(3, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvents().get(0).getType());
        assertEquals(last.getFixTime(), state.getEvents().get(0).getEventTime());
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvents().get(1).getType());
        assertEquals(last.getFixTime(), state.getEvents().get(1).getEventTime());
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvents().get(2).getType());
        assertEquals(current.getFixTime(), state.getEvents().get(2).getEventTime());
        assertEquals(current.getFixTime(), state.getEventTime());
        assertEquals(current.getLatitude(), state.getEventLatitude());
        assertEquals(current.getLongitude(), state.getEventLongitude());
    }

}
