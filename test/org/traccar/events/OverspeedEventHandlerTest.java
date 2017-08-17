package org.traccar.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.model.DeviceState;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class OverspeedEventHandlerTest  extends BaseTest {

    private Date date(String time) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.parse(time);
    }

    private void testOverspeedWithPosition(boolean notRepeat) throws Exception {
        OverspeedEventHandler overspeedEventHandler = new OverspeedEventHandler(15000, notRepeat);

        Position position = new Position();
        position.setTime(date("2017-01-01 00:00:00"));
        position.setSpeed(50);
        DeviceState deviceState = new DeviceState();
        deviceState.setOverspeedState(false);

        Map<Event, Position> events = overspeedEventHandler.updateOverspeedState(deviceState, position, 40);
        assertNull(events);
        assertFalse(deviceState.getOverspeedState());
        assertEquals(position, deviceState.getOverspeedPosition());

        Position nextPosition = new Position();
        nextPosition.setTime(date("2017-01-01 00:00:10"));
        nextPosition.setSpeed(55);

        events = overspeedEventHandler.updateOverspeedState(deviceState, nextPosition, 40);
        assertNull(events);

        nextPosition.setTime(date("2017-01-01 00:00:20"));

        events = overspeedEventHandler.updateOverspeedState(deviceState, nextPosition, 40);
        assertNotNull(events);
        Event event = events.keySet().iterator().next();
        assertEquals(Event.TYPE_DEVICE_OVERSPEED, event.getType());
        assertEquals(50, event.getDouble("speed"), 0.1);
        assertEquals(40, event.getDouble(OverspeedEventHandler.ATTRIBUTE_SPEED_LIMIT), 0.1);

        assertEquals(notRepeat, deviceState.getOverspeedState());
        assertNull(deviceState.getOverspeedPosition());

        nextPosition.setTime(date("2017-01-01 00:00:30"));
        events = overspeedEventHandler.updateOverspeedState(deviceState, nextPosition, 40);
        assertNull(events);
        assertEquals(notRepeat, deviceState.getOverspeedState());

        if (notRepeat) {
            assertNull(deviceState.getOverspeedPosition());
        } else {
            assertNotNull(deviceState.getOverspeedPosition());
        }

        nextPosition.setTime(date("2017-01-01 00:00:40"));
        nextPosition.setSpeed(30);

        events = overspeedEventHandler.updateOverspeedState(deviceState, nextPosition, 40);
        assertNull(events);
        assertFalse(deviceState.getOverspeedState());
        assertNull(deviceState.getOverspeedPosition());
    }

    private void testOverspeedWithStatus(boolean notRepeat) throws Exception {
        OverspeedEventHandler overspeedEventHandler = new OverspeedEventHandler(15000, notRepeat);

        Position position = new Position();
        position.setTime(new Date(System.currentTimeMillis() - 30000));
        position.setSpeed(50);
        DeviceState deviceState = new DeviceState();
        deviceState.setOverspeedState(false);
        deviceState.setOverspeedPosition(position);

        Map<Event, Position> events = overspeedEventHandler.updateOverspeedState(deviceState, 40);

        assertNotNull(events);
        Event event = events.keySet().iterator().next();
        assertEquals(Event.TYPE_DEVICE_OVERSPEED, event.getType());
        assertEquals(notRepeat, deviceState.getOverspeedState());
    }

    @Test
    public void testOverspeedEventHandler() throws Exception {
        testOverspeedWithPosition(false);
        testOverspeedWithPosition(true);

        testOverspeedWithStatus(false);
        testOverspeedWithStatus(true);
    }

}
