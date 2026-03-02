package org.traccar.session.state;

import org.junit.jupiter.api.Test;
import org.traccar.BaseTest;
import org.traccar.helper.DistanceCalculator;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
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
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, Long.MAX_VALUE);

        assertTrue(state.getMotionStreak());
        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvents().get(0).getType());
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
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, Long.MAX_VALUE);

        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvents().get(0).getType());
        assertFalse(state.getMotionStreak());
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
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, Long.MAX_VALUE);

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
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, Long.MAX_VALUE);

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
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, Long.MAX_VALUE);

        assertTrue(state.getMotionStreak());
        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvents().get(0).getType());
    }

    @Test
    public void testGapSplitsTrip() throws ParseException {
        double minDistance = 500;
        long minDuration = 300000;
        long stopGap = 3600000;

        double latitude = 0.0;
        double delta13000 = DistanceCalculator.getLongitudeDelta(13000, latitude);

        Deque<Position> positions = new ArrayDeque<>();
        positions.add(position("2017-01-01 00:00:00", latitude, 0.0));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 02:00:00", latitude, delta13000);
        current.set(Position.KEY_DISTANCE, 13000);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, stopGap);

        assertEquals(3, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvents().get(0).getType());
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvents().get(1).getType());
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvents().get(2).getType());
        assertFalse(state.getMotionStreak());
    }

    @Test
    public void testGapSplitsTripWhileStopped() throws ParseException {
        double minDistance = 500;
        long minDuration = 300000;
        long stopGap = 3600000;

        double latitude = 0.0;
        double delta13000 = DistanceCalculator.getLongitudeDelta(13000, latitude);

        Deque<Position> positions = new ArrayDeque<>();
        positions.add(position("2017-01-01 00:00:00", latitude, 0.0));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(false);
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 02:00:00", latitude, delta13000);
        current.set(Position.KEY_DISTANCE, 13000);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, stopGap);

        assertEquals(2, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvents().get(0).getType());
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvents().get(1).getType());
        assertFalse(state.getMotionStreak());
    }

    @Test
    public void testGapBelowDistanceDoesNotSplitTrip() throws ParseException {
        double minDistance = 500;
        long minDuration = 300000;
        long stopGap = 3600000;

        double latitude = 0.0;
        double delta100 = DistanceCalculator.getLongitudeDelta(100, latitude);

        Deque<Position> positions = new ArrayDeque<>();
        positions.add(position("2017-01-01 00:00:00", latitude, 0.0));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 02:00:00", latitude, delta100);
        current.set(Position.KEY_DISTANCE, 100);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, stopGap);

        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvents().get(0).getType());
        assertFalse(state.getMotionStreak());
    }

    @Test
    public void testEventsAreChronological() throws ParseException {
        double minDistance = 200;
        long minDuration = 180000;
        long stopGap = 3600000;

        double latitude = 0.0;
        List<Position> input = List.of(
                position("2017-01-01 00:00:00", latitude, DistanceCalculator.getLongitudeDelta(0, latitude)),
                position("2017-01-01 00:01:00", latitude, DistanceCalculator.getLongitudeDelta(0, latitude)),
                position("2017-01-01 00:02:00", latitude, DistanceCalculator.getLongitudeDelta(0, latitude)),
                position("2017-01-01 00:03:00", latitude, DistanceCalculator.getLongitudeDelta(100, latitude)),
                position("2017-01-01 00:04:00", latitude, DistanceCalculator.getLongitudeDelta(220, latitude)),
                position("2017-01-01 00:05:00", latitude, DistanceCalculator.getLongitudeDelta(0, latitude)),
                position("2017-01-01 00:06:00", latitude, DistanceCalculator.getLongitudeDelta(60, latitude)),
                position("2017-01-01 00:07:00", latitude, DistanceCalculator.getLongitudeDelta(320, latitude)));

        Deque<Position> positions = new ArrayDeque<>();
        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(false);
        state.setEventPosition(input.get(0));

        List<Event> events = new ArrayList<>();
        for (Position current : input) {
            NewMotionProcessor.updateState(state, current, minDistance, minDuration, stopGap);
            events.addAll(state.getEvents());

            positions.add(current);
            while (positions.size() > 1) {
                var iterator = positions.iterator();
                iterator.next();
                if (positions.peekLast().getFixTime().getTime() - iterator.next().getFixTime().getTime() >= minDuration) {
                    positions.poll();
                } else {
                    break;
                }
            }
        }

        assertEquals(3, events.size());
        assertEquals(Event.TYPE_DEVICE_MOVING, events.get(0).getType());
        assertEquals(input.get(4).getFixTime(), events.get(0).getEventTime());
        assertEquals(Event.TYPE_DEVICE_STOPPED, events.get(1).getType());
        assertEquals(input.get(6).getFixTime(), events.get(1).getEventTime());
        assertEquals(Event.TYPE_DEVICE_MOVING, events.get(2).getType());
        assertEquals(input.get(7).getFixTime(), events.get(2).getEventTime());
    }

}
