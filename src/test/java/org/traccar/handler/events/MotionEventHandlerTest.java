package org.traccar.handler.events;

import org.junit.jupiter.api.Test;
import org.traccar.BaseTest;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.reports.common.TripsConfig;
import org.traccar.session.state.MotionProcessor;
import org.traccar.session.state.MotionState;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MotionEventHandlerTest extends BaseTest {

    private Position position(String time, boolean motion, double distance, Boolean ignition) throws ParseException {
        Position position = new Position();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        position.setTime(dateFormat.parse(time));
        position.set(Position.KEY_MOTION, motion);
        position.set(Position.KEY_TOTAL_DISTANCE, distance);
        position.set(Position.KEY_IGNITION, ignition);
        return position;
    }

    private void verifyState(MotionState motionState, boolean state, long distance) {
        assertEquals(state, motionState.getMotionState());
        assertEquals(distance, motionState.getMotionDistance(), 0.1);
    }

    @Test
    public void testMotionWithPosition() throws ParseException {
        TripsConfig tripsConfig = new TripsConfig(500, 300000, 300000, 0, false, false);

        MotionState state = new MotionState();

        MotionProcessor.updateState(state, null, position("2017-01-01 00:00:00", false, 0, null), false, tripsConfig);
        assertNull(state.getEvent());
        verifyState(state, false, 0);

        MotionProcessor.updateState(state, null, position("2017-01-01 00:02:00", true, 100, null), true, tripsConfig);
        assertNull(state.getEvent());
        verifyState(state, true, 100);

        MotionProcessor.updateState(state, null, position("2017-01-01 00:02:00", true, 700, null), true, tripsConfig);
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvent().getType());
        verifyState(state, true, 0);

        MotionProcessor.updateState(state, null, position("2017-01-01 00:03:00", false, 700, null), false, tripsConfig);
        assertNull(state.getEvent());
        verifyState(state, false, 700);

        MotionProcessor.updateState(state, null, position("2017-01-01 00:10:00", false, 700, null), false, tripsConfig);
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvent().getType());
        verifyState(state, false, 0);
    }

    @Test
    public void testMotionFluctuation() throws ParseException {
        TripsConfig tripsConfig = new TripsConfig(500, 300000, 300000, 0, false, false);

        MotionState state = new MotionState();

        MotionProcessor.updateState(state, null, position("2017-01-01 00:00:00", false, 0, null), false, tripsConfig);
        assertNull(state.getEvent());
        verifyState(state, false, 0);

        MotionProcessor.updateState(state, null, position("2017-01-01 00:02:00", true, 100, null), true, tripsConfig);
        assertNull(state.getEvent());
        verifyState(state, true, 100);

        MotionProcessor.updateState(state, null, position("2017-01-01 00:02:00", true, 700, null), true, tripsConfig);
        assertEquals(Event.TYPE_DEVICE_MOVING, state.getEvent().getType());
        verifyState(state, true, 0);

        MotionProcessor.updateState(state, null, position("2017-01-01 00:03:00", false, 700, null), false, tripsConfig);
        assertNull(state.getEvent());
        verifyState(state, false, 700);

        MotionProcessor.updateState(state, null, position("2017-01-01 00:04:00", true, 1000, null), true, tripsConfig);
        assertNull(state.getEvent());
        verifyState(state, true, 0);

        MotionProcessor.updateState(state, null, position("2017-01-01 00:06:00", true, 2000, null), true, tripsConfig);
        assertNull(state.getEvent());
        verifyState(state, true, 0);
    }

    @Test
    public void testStopWithPositionIgnition() throws ParseException {
        TripsConfig tripsConfig = new TripsConfig(500, 300000, 300000, 0, true, false);

        MotionState state = new MotionState();
        state.setMotionStreak(true);
        state.setMotionState(true);

        MotionProcessor.updateState(state, null, position("2017-01-01 00:00:00", false, 100, true), false, tripsConfig);
        assertNull(state.getEvent());
        verifyState(state, false, 100);

        MotionProcessor.updateState(state, null, position("2017-01-01 00:02:00", false, 100, false), false, tripsConfig);
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvent().getType());
        verifyState(state, false, 0);
    }

    @Test
    public void testStopWithGap() throws ParseException {
        TripsConfig tripsConfig = new TripsConfig(500, 300000, 300000, 600000, false, false);

        MotionState state = new MotionState();
        state.setMotionState(true);
        state.setMotionStreak(true);

        Position lastPosition = position("2017-01-01 00:00:00", false, 100, null);
        MotionProcessor.updateState(state, null, lastPosition, false, tripsConfig);
        assertNull(state.getEvent());

        Position currentPosition = position("2017-01-01 00:10:00", false, 100, null);
        MotionProcessor.updateState(state, lastPosition, currentPosition, false, tripsConfig);
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvent().getType());
        assertEquals(lastPosition.getDeviceTime(), state.getEvent().getEventTime());
    }

    @Test
    public void testStopWithDoubleGap() throws ParseException {
        TripsConfig tripsConfig = new TripsConfig(500, 300000, 300000, 600000, false, false);

        MotionState state = new MotionState();
        state.setMotionState(true);
        state.setMotionStreak(true);

        Position firstPosition = position("2017-01-01 00:00:00", false, 100, null);
        MotionProcessor.updateState(state, null, firstPosition, false, tripsConfig);
        assertNull(state.getEvent());

        Position secondPosition = position("2017-01-01 00:10:00", false, 100, null);
        MotionProcessor.updateState(state, firstPosition, secondPosition, false, tripsConfig);
        assertEquals(Event.TYPE_DEVICE_STOPPED, state.getEvent().getType());
        assertEquals(firstPosition.getDeviceTime(), state.getEvent().getEventTime());

        Position thirdPosition = position("2017-01-01 00:20:00", false, 100, null);
        MotionProcessor.updateState(state, secondPosition, thirdPosition, false, tripsConfig);
        assertNull(state.getEvent());
    }

}
