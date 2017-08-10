package org.traccar.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.model.DeviceState;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.reports.model.TripsConfig;

public class MotionEventHandlerTest extends BaseTest {

    private Date date(String time) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.parse(time);
    }

    @Test
    public void testMotionEventHandler() throws Exception {
        TripsConfig tripsConfig = new TripsConfig(500, 300 * 1000, 300 * 1000, false, 0);

        Position position = new Position();
        position.setTime(date("2017-01-01 00:00:00"));
        position.set(Position.KEY_MOTION, true);
        position.set(Position.KEY_TOTAL_DISTANCE, 0);
        DeviceState deviceState = new DeviceState();
        deviceState.setMotionState(false);
        deviceState.setMotionPosition(position);
        Position nextPosition = new Position();

        nextPosition.setTime(date("2017-01-01 00:02:00"));
        nextPosition.set(Position.KEY_MOTION, true);
        nextPosition.set(Position.KEY_TOTAL_DISTANCE, 200);

        Event event = MotionEventHandler.updateMotionState(deviceState, nextPosition, tripsConfig);
        assertNull(event);

        nextPosition.set(Position.KEY_TOTAL_DISTANCE, 600);
        event = MotionEventHandler.updateMotionState(deviceState, nextPosition, tripsConfig);        
        assertNotNull(event);
        assertEquals(Event.TYPE_DEVICE_MOVING, event.getType());
        assertTrue(deviceState.getMotionState());
        assertNull(deviceState.getMotionPosition());

        deviceState.setMotionState(false);
        deviceState.setMotionPosition(position);
        nextPosition.setTime(date("2017-01-01 00:06:00"));
        nextPosition.set(Position.KEY_TOTAL_DISTANCE, 200);
        event = MotionEventHandler.updateMotionState(deviceState, nextPosition, tripsConfig);
        assertNotNull(event);
        assertEquals(Event.TYPE_DEVICE_MOVING, event.getType());
        assertTrue(deviceState.getMotionState());
        assertNull(deviceState.getMotionPosition());
    }

}
