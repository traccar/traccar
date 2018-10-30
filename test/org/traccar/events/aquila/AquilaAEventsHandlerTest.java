package org.traccar.events.aquila;

import org.junit.Test;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.Date;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by saurako on 5/24/18.
 */
public class AquilaAEventsHandlerTest {

    @Test
    public void testAquilaAEvents() {

        AquilaAEventsHandler aquilaAEventsHandler = new AquilaAEventsHandler();
        Position position = new Position();

        position.set(Position.KEY_CASE_OPEN, true);
        position.set(Position.KEY_EXTERNAL_BATTERY_DISCONNECT, true);
        position.setValid(true);
        position.setDeviceTime(new Date());
        Map<Event, Position> events = aquilaAEventsHandler.analyzePosition(position);

        assertEquals(2, events.size());
    }
}