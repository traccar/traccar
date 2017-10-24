package org.traccar.events;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class IgnitionEventHandlerTest extends BaseTest {
    
    @Test
    public void testIgnitionEventHandler() throws Exception {
        
        IgnitionEventHandler ignitionEventHandler = new IgnitionEventHandler();
        
        Position position = new Position();
        position.set(Position.KEY_IGNITION, true);
        position.setValid(true);
        Map<Event, Position> events = ignitionEventHandler.analyzePosition(position);
        assertEquals(events, null);
    }

}
