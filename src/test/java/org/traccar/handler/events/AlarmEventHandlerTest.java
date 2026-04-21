package org.traccar.handler.events;

import org.junit.jupiter.api.Test;
import org.traccar.BaseTest;
import org.traccar.config.Config;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

public class AlarmEventHandlerTest extends BaseTest {

    @Test
    public void testAlarmEventHandler() {
        
        AlarmEventHandler alarmEventHandler = new AlarmEventHandler(new Config(), mock(CacheManager.class));
        
        Position position = new Position();
        position.addAlarm(Position.ALARM_GENERAL);
        List<Event> events = new ArrayList<>();
        alarmEventHandler.analyzePosition(position, events::add);
        assertFalse(events.isEmpty());
        Event event = events.iterator().next();
        assertEquals(Event.TYPE_ALARM, event.getType());

    }

}
