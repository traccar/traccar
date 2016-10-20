package org.traccar.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class MotionEventHandlerTest extends BaseTest {
    
    @Test
    public void testMotionEventHandler() throws Exception {
        
        MotionEventHandler motionEventHandler = new MotionEventHandler();
        
        Position position = new Position();
        position.setSpeed(10.0);
        position.setValid(true);
        Collection<Event> events = motionEventHandler.analyzePosition(position);
        assertNotNull(events);
        Event event = (Event) events.toArray()[0];
        assertEquals(Event.TYPE_DEVICE_MOVING, event.getType());
    }

}
