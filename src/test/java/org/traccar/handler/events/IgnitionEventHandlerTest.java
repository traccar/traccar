package org.traccar.handler.events;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.database.IdentityManager;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class IgnitionEventHandlerTest extends BaseTest {
    
    @Test
    public void testIgnitionEventHandler() {
        
        IgnitionEventHandler ignitionEventHandler = new IgnitionEventHandler(mock(IdentityManager.class));
        
        Position position = new Position();
        position.set(Position.KEY_IGNITION, true);
        position.setValid(true);
        Map<Event, Position> events = ignitionEventHandler.analyzePosition(position);
        assertNull(events);
    }

}
