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
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 00:05:00", latitude, delta600);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration);

        assertTrue(state.getMotionStreak());
        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvents().get(0).getType());
        assertEquals(positions.peekLast().getFixTime(), state.getEvents().get(0).getEventTime());
        assertEquals(positions.peekLast().getFixTime(), state.getEventTime());
        assertEquals(positions.peekLast().getLatitude(), state.getEventLatitude());
        assertEquals(positions.peekLast().getLongitude(), state.getEventLongitude());
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
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 00:06:00", latitude, delta100);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration);

        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvents().get(0).getType());
        assertEquals(positions.peekFirst().getFixTime(), state.getEvents().get(0).getEventTime());
        assertFalse(state.getMotionStreak());
        assertEquals(positions.peekFirst().getFixTime(), state.getEventTime());
        assertEquals(positions.peekFirst().getLatitude(), state.getEventLatitude());
        assertEquals(positions.peekFirst().getLongitude(), state.getEventLongitude());
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
        state.setEventPosition(positions.peekFirst());

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
        state.setEventPosition(positions.peekFirst());

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
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 00:10:00", latitude, delta1200);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration);

        assertTrue(state.getMotionStreak());
        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvents().get(0).getType());
        assertEquals(positions.peekFirst().getFixTime(), state.getEvents().get(0).getEventTime());
    }

}
