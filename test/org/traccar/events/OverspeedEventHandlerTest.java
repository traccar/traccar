package org.traccar.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

public class OverspeedEventHandlerTest  extends BaseTest {

    private Date date(String time) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.parse(time);
    }

    private void testOverspeed(boolean notRepeat) throws Exception {
        Position position = new Position();
        position.setTime(date("2017-01-01 00:00:00"));
        position.setSpeed(50);
        DeviceState deviceState = new DeviceState();
        deviceState.setOverspeedState(false);

        Event event = OverspeedEventHandler.updateOverspeedState(deviceState, position, 40, 15000, notRepeat);
        assertNull(event);
        assertFalse(deviceState.getOverspeedState());
        assertEquals(position, deviceState.getOverspeedPosition());

        Position nextPosition = new Position();
        nextPosition.setTime(date("2017-01-01 00:00:10"));
        nextPosition.setSpeed(55);

        event = OverspeedEventHandler.updateOverspeedState(deviceState, nextPosition, 40, 15000, notRepeat);
        assertNull(event);

        nextPosition.setTime(date("2017-01-01 00:00:20"));

        event = OverspeedEventHandler.updateOverspeedState(deviceState, nextPosition, 40, 15000, notRepeat);
        assertNotNull(event);
        assertEquals(Event.TYPE_DEVICE_OVERSPEED, event.getType());
        assertEquals(50, event.getDouble("speed"), 0.1);
        assertEquals(40, event.getDouble(OverspeedEventHandler.ATTRIBUTE_SPEED_LIMIT), 0.1);

        assertEquals(notRepeat, deviceState.getOverspeedState());
        assertNull(deviceState.getOverspeedPosition());
        
        nextPosition.setTime(date("2017-01-01 00:00:30"));
        event = OverspeedEventHandler.updateOverspeedState(deviceState, nextPosition, 40, 15000, notRepeat);
        assertNull(event);
        assertEquals(notRepeat, deviceState.getOverspeedState());
        
        if (notRepeat) {
            assertNull(deviceState.getOverspeedPosition());
        } else {
            assertNotNull(deviceState.getOverspeedPosition());
        }

        nextPosition.setTime(date("2017-01-01 00:00:40"));
        nextPosition.setSpeed(30);

        event = OverspeedEventHandler.updateOverspeedState(deviceState, nextPosition, 40, 15000, notRepeat);
        assertNull(event);
        assertFalse(deviceState.getOverspeedState());
        assertNull(deviceState.getOverspeedPosition());
    }

    @Test
    public void testOverspeedEventHandler() throws Exception {
        testOverspeed(false);
        testOverspeed(true);
    }

}
