package org.traccar.session.state;

import org.junit.jupiter.api.Test;
import org.traccar.BaseTest;
import org.traccar.helper.DistanceCalculator;
import org.traccar.model.CompactPosition;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
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

        Deque<CompactPosition> positions = new ArrayDeque<>();
        positions.add(new CompactPosition(position("2017-01-01 00:00:00", latitude, 0.0)));
        positions.add(new CompactPosition(position("2017-01-01 00:02:00", latitude, 0.0)));
        positions.add(new CompactPosition(position("2017-01-01 00:04:00", latitude, 0.0)));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 00:05:00", latitude, delta600);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, Long.MAX_VALUE, false);

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

        Deque<CompactPosition> positions = new ArrayDeque<>();
        positions.add(new CompactPosition(position("2017-01-01 00:00:00", latitude, 0.0)));
        positions.add(new CompactPosition(position("2017-01-01 00:02:00", latitude, delta100)));
        positions.add(new CompactPosition(position("2017-01-01 00:04:00", latitude, delta100)));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 00:06:00", latitude, delta100);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, Long.MAX_VALUE, false);

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

        Deque<CompactPosition> positions = new ArrayDeque<>();
        positions.add(new CompactPosition(position("2017-01-01 00:00:00", latitude, 0.0)));
        positions.add(new CompactPosition(position("2017-01-01 00:01:00", latitude, delta100)));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 00:02:00", latitude, delta100);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, Long.MAX_VALUE, false);

        assertTrue(state.getEvents().isEmpty());
        assertTrue(state.getMotionStreak());
    }

    @Test
    public void testNoEventWhenAlreadyMoving() throws ParseException {
        double minDistance = 500;
        long minDuration = 300000;

        double latitude = 0.0;
        double delta600 = DistanceCalculator.getLongitudeDelta(600, latitude);

        Deque<CompactPosition> positions = new ArrayDeque<>();
        positions.add(new CompactPosition(position("2017-01-01 00:00:00", latitude, 0.0)));
        positions.add(new CompactPosition(position("2017-01-01 00:02:00", latitude, 0.0)));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 00:03:00", latitude, delta600);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, Long.MAX_VALUE, false);

        assertTrue(state.getEvents().isEmpty());
        assertTrue(state.getMotionStreak());
    }

    @Test
    public void testMotionWithFastAverageSpeed() throws ParseException {
        double minDistance = 500;
        long minDuration = 300000;

        double latitude = 0.0;
        double delta1200 = DistanceCalculator.getLongitudeDelta(1200, latitude);

        Deque<CompactPosition> positions = new ArrayDeque<>();
        positions.add(new CompactPosition(position("2017-01-01 00:00:00", latitude, 0.0)));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 00:10:00", latitude, delta1200);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, Long.MAX_VALUE, false);

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

        Deque<CompactPosition> positions = new ArrayDeque<>();
        positions.add(new CompactPosition(position("2017-01-01 00:00:00", latitude, 0.0)));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 02:00:00", latitude, delta13000);
        current.set(Position.KEY_DISTANCE, 13000);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, stopGap, false);

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

        Deque<CompactPosition> positions = new ArrayDeque<>();
        positions.add(new CompactPosition(position("2017-01-01 00:00:00", latitude, 0.0)));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(false);
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 02:00:00", latitude, delta13000);
        current.set(Position.KEY_DISTANCE, 13000);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, stopGap, false);

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

        Deque<CompactPosition> positions = new ArrayDeque<>();
        positions.add(new CompactPosition(position("2017-01-01 00:00:00", latitude, 0.0)));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 02:00:00", latitude, delta100);
        current.set(Position.KEY_DISTANCE, 100);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, stopGap, false);

        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvents().get(0).getType());
        assertFalse(state.getMotionStreak());
    }

    @Test
    public void testTripStartBacktrack() throws ParseException {
        double minDistance = 500;
        long minDuration = 300000;

        double latitude = 0.0;

        Position stop = position("2017-01-01 00:00:00", latitude, 0.0);
        Deque<CompactPosition> positions = new ArrayDeque<>();
        positions.add(new CompactPosition(stop));
        positions.add(new CompactPosition(position("2017-01-01 00:01:00", latitude, 0.0)));
        Position departure = position("2017-01-01 00:02:00", latitude, 0.0);
        positions.add(new CompactPosition(departure));
        positions.add(new CompactPosition(position("2017-01-01 00:03:00", latitude,
                DistanceCalculator.getLongitudeDelta(120, latitude))));
        positions.add(new CompactPosition(position("2017-01-01 00:04:00", latitude,
                DistanceCalculator.getLongitudeDelta(300, latitude))));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setEventPosition(stop);

        Position current = position("2017-01-01 00:05:00", latitude,
                DistanceCalculator.getLongitudeDelta(600, latitude));
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, Long.MAX_VALUE, false);

        assertTrue(state.getMotionStreak());
        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvents().get(0).getType());
        assertEquals(departure.getFixTime(), state.getEvents().get(0).getEventTime());
    }

    private static Position positionAt(long time, double latitude, double longitude) {
        Position position = new Position();
        position.setTime(new Date(time));
        position.setLatitude(latitude);
        position.setLongitude(longitude);
        position.setDeviceId(1);
        return position;
    }

    @Test
    public void testStopBacktrackIgnoresSimultaneousRecords() throws ParseException {
        double latitude = 0.0;
        double stop = DistanceCalculator.getLongitudeDelta(1000, latitude);
        double jitter = DistanceCalculator.getLongitudeDelta(1003, latitude);

        Position arrival = position("2017-01-01 00:01:00", latitude, stop);
        Position periodic = position("2017-01-01 00:36:00", latitude, stop);
        Deque<CompactPosition> positions = new ArrayDeque<>();
        positions.add(new CompactPosition(arrival));
        positions.add(new CompactPosition(periodic));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);
        state.setEventPosition(position("2016-12-31 23:50:00", latitude, 0.0));

        Position current = positionAt(periodic.getFixTime().getTime() + 10, latitude, jitter);
        NewMotionProcessor.updateState(state, current, 200, 180000, Long.MAX_VALUE, false);

        assertFalse(state.getMotionStreak());
        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvents().get(0).getType());
        assertEquals(arrival.getFixTime(), state.getEvents().get(0).getEventTime());
    }

    @Test
    public void testStopBacktrackWithSubSecondReporting() throws ParseException {
        double latitude = 0.0;
        long start = position("2017-01-01 00:00:00", latitude, 0.0).getFixTime().getTime();
        long arrival = start + 10000;
        long now = arrival + 180000;
        double stop = DistanceCalculator.getLongitudeDelta(1000, latitude);

        Deque<CompactPosition> positions = new ArrayDeque<>();
        for (long time = start; time < now; time += 500) {
            double meters = time < arrival ? 15 * (arrival - time) / 1000.0 : 0;
            positions.add(new CompactPosition(positionAt(time, latitude,
                    stop - DistanceCalculator.getLongitudeDelta(meters, latitude))));
        }

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(true);
        state.setEventPosition(position("2016-12-31 23:50:00", latitude, 0.0));

        NewMotionProcessor.updateState(state, positionAt(now, latitude, stop), 200, 180000, Long.MAX_VALUE, false);

        assertFalse(state.getMotionStreak());
        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvents().get(0).getType());
        assertTrue(Math.abs(state.getEvents().get(0).getEventTime().getTime() - arrival) <= 1000);
    }

    @Test
    public void testStartBacktrackWithSubSecondReporting() throws ParseException {
        double latitude = 0.0;
        Position stop = position("2017-01-01 00:00:00", latitude, 0.0);
        long departure = stop.getFixTime().getTime() + 60000;
        long now = departure + 20000;

        Deque<CompactPosition> positions = new ArrayDeque<>();
        for (long time = stop.getFixTime().getTime() + 500; time < now; time += 500) {
            double meters = time > departure ? 15 * (time - departure) / 1000.0 : 0;
            positions.add(new CompactPosition(positionAt(time, latitude,
                    DistanceCalculator.getLongitudeDelta(meters, latitude))));
        }

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setEventPosition(stop);

        NewMotionProcessor.updateState(state, positionAt(now, latitude,
                DistanceCalculator.getLongitudeDelta(300, latitude)), 200, 180000, Long.MAX_VALUE, false);

        assertTrue(state.getMotionStreak());
        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvents().get(0).getType());
        assertTrue(Math.abs(state.getEvents().get(0).getEventTime().getTime() - departure) <= 1000);
    }

    @Test
    public void testStartBacktrackDoesNotBridgeGapAfterSubSecondLeg() throws ParseException {
        double latitude = 0.0;
        Position stop = position("2017-01-01 00:00:00", latitude, 0.0);
        Position periodic = position("2017-01-01 01:00:00", latitude, 0.0);
        Position beforeThreshold = position("2017-01-01 02:00:00", latitude,
                DistanceCalculator.getLongitudeDelta(199, latitude));
        Position current = positionAt(beforeThreshold.getFixTime().getTime() + 10, latitude,
                DistanceCalculator.getLongitudeDelta(201, latitude));

        Deque<CompactPosition> positions = new ArrayDeque<>();
        positions.add(new CompactPosition(periodic));
        positions.add(new CompactPosition(beforeThreshold));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setEventPosition(stop);

        NewMotionProcessor.updateState(state, current, 200, 180000, Long.MAX_VALUE, false);

        assertTrue(state.getMotionStreak());
        assertEquals(1, state.getEvents().size());
        Event event = state.getEvents().get(0);
        assertEquals(Event.TYPE_DEVICE_MOVING, event.getType());
        assertFalse(event.getEventTime().before(beforeThreshold.getFixTime()));
        assertFalse(event.getEventTime().after(current.getFixTime()));
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

        Deque<CompactPosition> positions = new ArrayDeque<>();
        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setMotionStreak(false);
        state.setEventPosition(input.get(0));

        List<Event> events = new ArrayList<>();
        for (Position current : input) {
            NewMotionProcessor.updateState(state, current, minDistance, minDuration, stopGap, false);
            events.addAll(state.getEvents());

            positions.add(new CompactPosition(current));
            while (positions.size() > 1) {
                var iterator = positions.iterator();
                iterator.next();
                if (positions.peekLast().fixTime().getTime() - iterator.next().fixTime().getTime() >= minDuration) {
                    positions.poll();
                } else {
                    break;
                }
            }
        }

        assertEquals(3, events.size());
        assertEquals(Event.TYPE_DEVICE_MOVING, events.get(0).getType());
        assertEquals(input.get(2).getFixTime(), events.get(0).getEventTime());
        assertEquals(Event.TYPE_DEVICE_STOPPED, events.get(1).getType());
        assertEquals(input.get(5).getFixTime(), events.get(1).getEventTime());
        assertEquals(Event.TYPE_DEVICE_MOVING, events.get(2).getType());
        assertEquals(input.get(6).getFixTime(), events.get(2).getEventTime());
    }

    @Test
    public void testMotionDetectedWithIgnition() throws ParseException {
        double minDistance = 500;
        long minDuration = 300000;

        double latitude = 0.0;

        Deque<CompactPosition> positions = new ArrayDeque<>();
        positions.add(new CompactPosition(position("2017-01-01 00:00:00", latitude, 0.0)));

        NewMotionState state = new NewMotionState();
        state.setPositions(positions);
        state.setEventPosition(positions.peekFirst());

        Position current = position("2017-01-01 00:01:00", latitude, 0.0);
        current.set(Position.KEY_IGNITION, true);
        NewMotionProcessor.updateState(state, current, minDistance, minDuration, Long.MAX_VALUE, true);

        assertTrue(state.getMotionStreak());
        assertEquals(1, state.getEvents().size());
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvents().get(0).getType());
        assertEquals(current.getFixTime(), state.getEvents().get(0).getEventTime());
    }

}
