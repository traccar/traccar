package org.traccar.handler.events;

import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.config.Config;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class AlertEventHandlerTest extends BaseTest {

    @Test
    public void testAlertEventHandler() {
        
        AlertEventHandler alertEventHandler = new AlertEventHandler(new Config(), mock(CacheManager.class));
        
        Position position = new Position();
        position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
        Map<Event, Position> events = alertEventHandler.analyzePosition(position);
        assertNotNull(events);
        Event event = events.keySet().iterator().next();
        assertEquals(Event.TYPE_ALARM, event.getType());

    }

}
