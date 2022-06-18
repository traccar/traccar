package org.traccar.handler.events;

import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.session.DeviceState;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class OverspeedEventHandlerTest  extends BaseTest {

    private Date date(String time) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.parse(time);
    }

    private void testOverspeedWithPosition(boolean notRepeat, long geofenceId) throws ParseException {
        Config config = new Config();
        config.setString(Keys.EVENT_OVERSPEED_NOT_REPEAT, String.valueOf(notRepeat));
        config.setString(Keys.EVENT_OVERSPEED_MINIMAL_DURATION, String.valueOf(15));
        config.setString(Keys.EVENT_OVERSPEED_PREFER_LOWEST, String.valueOf(false));
        OverspeedEventHandler overspeedEventHandler = new OverspeedEventHandler(config, null, null);

        Position position = new Position();
        position.setTime(date("2017-01-01 00:00:00"));
        position.setSpeed(50);
        DeviceState deviceState = new DeviceState();
        deviceState.setOverspeedState(false);

        Map<Event, Position> events = overspeedEventHandler.updateOverspeedState(deviceState, position, 40, geofenceId);
        assertNull(events);
        assertFalse(deviceState.getOverspeedState());
        assertEquals(position, deviceState.getOverspeedPosition());
        assertEquals(geofenceId, deviceState.getOverspeedGeofenceId());

        Position nextPosition = new Position();
        nextPosition.setTime(date("2017-01-01 00:00:10"));
        nextPosition.setSpeed(55);

        events = overspeedEventHandler.updateOverspeedState(deviceState, nextPosition, 40, geofenceId);
        assertNull(events);

        nextPosition.setTime(date("2017-01-01 00:00:20"));

        events = overspeedEventHandler.updateOverspeedState(deviceState, nextPosition, 40, geofenceId);
        assertNotNull(events);
        Event event = events.keySet().iterator().next();
        assertEquals(Event.TYPE_DEVICE_OVERSPEED, event.getType());
        assertEquals(50, event.getDouble("speed"), 0.1);
        assertEquals(40, event.getDouble("speedLimit"), 0.1);
        assertEquals(geofenceId, event.getGeofenceId());

        assertEquals(notRepeat, deviceState.getOverspeedState());
        assertNull(deviceState.getOverspeedPosition());
        assertEquals(0, deviceState.getOverspeedGeofenceId());

        nextPosition.setTime(date("2017-01-01 00:00:30"));
        events = overspeedEventHandler.updateOverspeedState(deviceState, nextPosition, 40, geofenceId);
        assertNull(events);
        assertEquals(notRepeat, deviceState.getOverspeedState());

        if (notRepeat) {
            assertNull(deviceState.getOverspeedPosition());
            assertEquals(0, deviceState.getOverspeedGeofenceId());
        } else {
            assertNotNull(deviceState.getOverspeedPosition());
            assertEquals(geofenceId, deviceState.getOverspeedGeofenceId());
        }

        nextPosition.setTime(date("2017-01-01 00:00:40"));
        nextPosition.setSpeed(30);

        events = overspeedEventHandler.updateOverspeedState(deviceState, nextPosition, 40, geofenceId);
        assertNull(events);
        assertFalse(deviceState.getOverspeedState());
        assertNull(deviceState.getOverspeedPosition());
        assertEquals(0, deviceState.getOverspeedGeofenceId());
    }

    private void testOverspeedWithStatus(boolean notRepeat) {
        Config config = new Config();
        config.setString(Keys.EVENT_OVERSPEED_NOT_REPEAT, String.valueOf(notRepeat));
        config.setString(Keys.EVENT_OVERSPEED_MINIMAL_DURATION, String.valueOf(15));
        config.setString(Keys.EVENT_OVERSPEED_PREFER_LOWEST, String.valueOf(false));
        OverspeedEventHandler overspeedEventHandler = new OverspeedEventHandler(config, null, null);

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
        testOverspeedWithPosition(false, 0);
        testOverspeedWithPosition(true, 0);
        
        testOverspeedWithPosition(false, 1);
        testOverspeedWithPosition(true, 1);

        testOverspeedWithStatus(false);
        testOverspeedWithStatus(true);
    }

}
