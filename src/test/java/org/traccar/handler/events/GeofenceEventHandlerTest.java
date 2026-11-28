package org.traccar.handler.events;

import org.junit.jupiter.api.Test;
import org.traccar.BaseTest;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Order;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeofenceEventHandlerTest extends BaseTest {

    private static Position position(long deviceId, long time, double latitude, double longitude) {
        Position position = new Position();
        position.setDeviceId(deviceId);
        position.setFixTime(new Date(time));
        position.setDeviceTime(new Date(time));
        position.setLatitude(latitude);
        position.setLongitude(longitude);
        return position;
    }

    @Test
    public void testOrderVisitOnEntryAndReentry() {
        Geofence geofence = new Geofence();
        geofence.setId(20);
        geofence.setArea("CIRCLE (55.0 37.0, 100)");

        Order order = new Order();
        order.setId(10);
        order.setGeofenceId(20);

        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getDeviceObjects(1, Geofence.class)).thenReturn(Set.of(geofence));
        when(cacheManager.getDeviceObjects(1, Order.class)).thenReturn(Set.of(order));
        when(cacheManager.getObject(Geofence.class, 20)).thenReturn(geofence);

        GeofenceEventHandler handler = new GeofenceEventHandler(new Config(), cacheManager);
        List<Event> events = new ArrayList<>();

        Position outside = position(1, 1, 55.01, 37.01);
        Position inside = position(1, 2, 55.0, 37.0);
        Position insideAgain = position(1, 3, 55.0, 37.0);
        Position outsideAgain = position(1, 4, 55.01, 37.01);
        Position insideAgainAfterExit = position(1, 5, 55.0, 37.0);

        when(cacheManager.getPosition(anyLong())).thenReturn(null);
        handler.analyzePosition(outside, events::add);

        when(cacheManager.getPosition(anyLong())).thenReturn(outside);
        inside.setGeofenceIds(List.of(20L));
        handler.analyzePosition(inside, events::add);

        when(cacheManager.getPosition(anyLong())).thenReturn(inside);
        insideAgain.setGeofenceIds(List.of(20L));
        handler.analyzePosition(insideAgain, events::add);

        when(cacheManager.getPosition(anyLong())).thenReturn(insideAgain);
        handler.analyzePosition(outsideAgain, events::add);

        when(cacheManager.getPosition(anyLong())).thenReturn(outsideAgain);
        insideAgainAfterExit.setGeofenceIds(List.of(20L));
        handler.analyzePosition(insideAgainAfterExit, events::add);

        assertEquals(5, events.size());
        assertEquals(2, events.stream().filter(event -> Event.TYPE_ORDER_VISIT.equals(event.getType())).count());
        assertTrue(events.stream().filter(event -> Event.TYPE_ORDER_VISIT.equals(event.getType()))
                .allMatch(event -> event.getOrderId() == 10 && event.getGeofenceId() == 20));
    }

    @Test
    public void testOrderVisitOnSegmentCrossing() {
        Config config = new Config();
        config.setString(Keys.EVENT_GEOFENCE_SEGMENT_CROSSING, "true");

        Geofence geofence = new Geofence();
        geofence.setId(20);
        geofence.setArea("CIRCLE (55.0 37.0, 100)");

        Order order = new Order();
        order.setId(10);
        order.setGeofenceId(20);

        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getDeviceObjects(1, Geofence.class)).thenReturn(Set.of(geofence));
        when(cacheManager.getDeviceObjects(1, Order.class)).thenReturn(Set.of(order));

        GeofenceEventHandler handler = new GeofenceEventHandler(config, cacheManager);
        Position before = position(1, 1, 54.998, 37.0);
        Position after = position(1, 2, 55.002, 37.0);
        when(cacheManager.getPosition(anyLong())).thenReturn(before);

        List<Event> events = new ArrayList<>();
        handler.analyzePosition(after, events::add);

        assertEquals(1, events.stream().filter(event -> Event.TYPE_ORDER_VISIT.equals(event.getType())).count());
    }

    @Test
    public void testUnassociatedAndOutOfOrderPositions() {
        Geofence geofence = new Geofence();
        geofence.setId(20);
        geofence.setArea("CIRCLE (55.0 37.0, 100)");

        Order order = new Order();
        order.setId(10);
        order.setGeofenceId(21);

        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getDeviceObjects(1, Geofence.class)).thenReturn(Set.of(geofence));
        when(cacheManager.getDeviceObjects(1, Order.class)).thenReturn(Set.of(order));
        when(cacheManager.getObject(Geofence.class, 20)).thenReturn(geofence);

        GeofenceEventHandler handler = new GeofenceEventHandler(new Config(), cacheManager);
        Position last = position(1, 2, 55.01, 37.01);
        Position outOfOrder = position(1, 1, 55.0, 37.0);
        outOfOrder.setGeofenceIds(List.of(20L));
        when(cacheManager.getPosition(anyLong())).thenReturn(last);

        List<Event> events = new ArrayList<>();
        handler.analyzePosition(outOfOrder, events::add);

        assertTrue(events.isEmpty());

        Position current = position(1, 3, 55.0, 37.0);
        current.setGeofenceIds(List.of(20L));
        when(cacheManager.getPosition(anyLong())).thenReturn(last);
        handler.analyzePosition(current, events::add);

        assertEquals(1, events.size());
        assertEquals(Event.TYPE_GEOFENCE_ENTER, events.getFirst().getType());
    }

}
