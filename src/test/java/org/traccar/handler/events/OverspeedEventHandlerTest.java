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
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class OverspeedEventHandlerTest  extends BaseTest {

    private Position position(String time, double speed) throws ParseException {
        Position position = new Position();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        position.setTime(dateFormat.parse(time));
        position.setSpeed(speed);
        return position;
    }

    private void verifyState(DeviceState deviceState, boolean state, long geofenceId) {
        assertEquals(state, deviceState.getOverspeedState());
        assertEquals(geofenceId, deviceState.getOverspeedGeofenceId());
    }

    private void testOverspeedWithPosition(long geofenceId) throws ParseException {
        Config config = new Config();
        config.setString(Keys.EVENT_OVERSPEED_MINIMAL_DURATION, String.valueOf(15));
        config.setString(Keys.EVENT_OVERSPEED_PREFER_LOWEST, String.valueOf(false));
        OverspeedEventHandler overspeedEventHandler = new OverspeedEventHandler(config, null, null);

        DeviceState deviceState = new DeviceState();

        Position position = position("2017-01-01 00:00:00", 50);
        assertNull(overspeedEventHandler.updateOverspeedState(deviceState, position, 40, geofenceId));
        verifyState(deviceState, true, geofenceId);

        position = position("2017-01-01 00:00:10", 55);
        assertNull(overspeedEventHandler.updateOverspeedState(deviceState, position, 40, geofenceId));

        position = position("2017-01-01 00:00:20", 55);
        var events = overspeedEventHandler.updateOverspeedState(deviceState, position, 40, geofenceId);
        assertNotNull(events);
        Event event = events.keySet().iterator().next();
        assertEquals(Event.TYPE_DEVICE_OVERSPEED, event.getType());
        assertEquals(55, event.getDouble("speed"), 0.1);
        assertEquals(40, event.getDouble("speedLimit"), 0.1);
        assertEquals(geofenceId, event.getGeofenceId());
        verifyState(deviceState, true, 0);

        position = position("2017-01-01 00:00:30", 55);
        assertNull(overspeedEventHandler.updateOverspeedState(deviceState, position, 40, geofenceId));
        verifyState(deviceState, true, 0);

        position = position("2017-01-01 00:00:30", 30);
        assertNull(overspeedEventHandler.updateOverspeedState(deviceState, position, 40, geofenceId));
        verifyState(deviceState, false, 0);
    }

    @Test
    public void testOverspeedEventHandler() throws Exception {
        testOverspeedWithPosition(0);
        testOverspeedWithPosition(1);
    }

}
