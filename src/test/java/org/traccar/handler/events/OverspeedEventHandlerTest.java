package org.traccar.handler.events;

import org.junit.jupiter.api.Test;
import org.traccar.BaseTest;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.session.state.OverspeedProcessor;
import org.traccar.session.state.OverspeedState;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OverspeedEventHandlerTest  extends BaseTest {

    private Position position(String time, double speed) throws ParseException {
        Position position = new Position();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        position.setTime(dateFormat.parse(time));
        position.setSpeed(speed);
        return position;
    }

    private void verifyState(OverspeedState overspeedState, boolean state, long geofenceId) {
        assertEquals(state, overspeedState.getOverspeedState());
        assertEquals(geofenceId, overspeedState.getOverspeedGeofenceId());
    }

    private void testOverspeedWithPosition(long geofenceId) throws ParseException {
        OverspeedState state = new OverspeedState();

        OverspeedProcessor.updateState(state, position("2017-01-01 00:00:00", 50), 40, 15000, geofenceId);
        assertNull(state.getEvent());
        verifyState(state, true, geofenceId);

        OverspeedProcessor.updateState(state, position("2017-01-01 00:00:10", 55), 40, 15000, geofenceId);
        assertNull(state.getEvent());

        OverspeedProcessor.updateState(state, position("2017-01-01 00:00:20", 55), 40, 15000, geofenceId);
        assertNotNull(state.getEvent());
        assertEquals(Event.TYPE_DEVICE_OVERSPEED, state.getEvent().getType());
        assertEquals(55, state.getEvent().getDouble("speed"), 0.1);
        assertEquals(40, state.getEvent().getDouble("speedLimit"), 0.1);
        assertEquals(geofenceId, state.getEvent().getGeofenceId());
        verifyState(state, true, 0);

        OverspeedProcessor.updateState(state, position("2017-01-01 00:00:30", 55), 40, 15000, geofenceId);
        assertNull(state.getEvent());
        verifyState(state, true, 0);

        OverspeedProcessor.updateState(state, position("2017-01-01 00:00:30", 30), 40, 15000, geofenceId);
        assertNull(state.getEvent());
        verifyState(state, false, 0);
    }

    @Test
    public void testOverspeedEventHandler() throws Exception {
        testOverspeedWithPosition(0);
        testOverspeedWithPosition(1);
    }

}
