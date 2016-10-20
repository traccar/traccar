package org.traccar.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class AlertEventHandlerTest extends BaseTest {

    @Test
    public void testAlertEventHandler() throws Exception {
        
        AlertEventHandler alertEventHandler = new AlertEventHandler();
        
        Position position = new Position();
        position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
        Collection<Event> events = alertEventHandler.analyzePosition(position);
        assertNotNull(events);
        Event event = (Event) events.toArray()[0];
        assertEquals(Event.TYPE_ALARM, event.getType());
    }

}
