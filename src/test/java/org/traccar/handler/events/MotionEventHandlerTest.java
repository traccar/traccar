package org.traccar.handler.events;

import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.reports.common.TripsConfig;
import org.traccar.session.DeviceState;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    private void verifyState(DeviceState deviceState, boolean state, long distance) {
        assertEquals(state, deviceState.getMotionState());
        assertEquals(distance, deviceState.getMotionDistance(), 0.1);
    }

    @Test
    public void testMotionWithPosition() throws ParseException {
        MotionEventHandler motionEventHandler = new MotionEventHandler(
                null, null, new TripsConfig(500, 300000, 300000, 0, false, false, 0.01));

        DeviceState deviceState = new DeviceState();

        Position position = position("2017-01-01 00:00:00", false, 0, null);
        assertNull(motionEventHandler.updateMotionState(deviceState, position, position.getBoolean(Position.KEY_MOTION)));
        verifyState(deviceState, false, 0);

        position = position("2017-01-01 00:02:00", true, 100, null);
        assertNull(motionEventHandler.updateMotionState(deviceState, position, position.getBoolean(Position.KEY_MOTION)));
        verifyState(deviceState, true, 100);

        position = position("2017-01-01 00:02:00", true, 700, null);
        var events = motionEventHandler.updateMotionState(deviceState, position, position.getBoolean(Position.KEY_MOTION));
        assertEquals(Event.TYPE_DEVICE_MOVING, events.keySet().iterator().next().getType());
        verifyState(deviceState, true, 0);

        position = position("2017-01-01 00:03:00", false, 700, null);
        assertNull(motionEventHandler.updateMotionState(deviceState, position, position.getBoolean(Position.KEY_MOTION)));
        verifyState(deviceState, false, 700);

        position = position("2017-01-01 00:10:00", false, 700, null);
        events = motionEventHandler.updateMotionState(deviceState, position, position.getBoolean(Position.KEY_MOTION));
        assertEquals(Event.TYPE_DEVICE_STOPPED, events.keySet().iterator().next().getType());
        verifyState(deviceState, false, 0);
    }

    @Test
    public void testStopWithPositionIgnition() throws ParseException {
        MotionEventHandler motionEventHandler = new MotionEventHandler(
                null, null, new TripsConfig(500, 300000, 300000, 0, true, false, 0.01));

        DeviceState deviceState = new DeviceState();
        deviceState.setMotionState(true);

        Position position = position("2017-01-01 00:00:00", false, 100, true);
        assertNull(motionEventHandler.updateMotionState(deviceState, position, position.getBoolean(Position.KEY_MOTION)));
        verifyState(deviceState, false, 100);

        position = position("2017-01-01 00:02:00", false, 100, false);
        var events = motionEventHandler.updateMotionState(deviceState, position, position.getBoolean(Position.KEY_MOTION));
        assertEquals(Event.TYPE_DEVICE_STOPPED, events.keySet().iterator().next().getType());
        verifyState(deviceState, false, 0);
    }

}
