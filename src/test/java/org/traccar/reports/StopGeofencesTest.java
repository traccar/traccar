package org.traccar.reports;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.traccar.config.Config;
import org.traccar.helper.model.StopGeofences;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Permission;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.reports.common.ReportUtils;
import org.traccar.reports.model.StopReportItem;
import org.traccar.storage.DatabaseStorage;
import org.traccar.storage.MemoryStorage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class StopGeofencesTest {

    private final MemoryStorage storage = new MemoryStorage();
    private final Request all = new Request(new Columns.All());
    private final long user = 100;

    private long addGeofence(String name, boolean visible) {
        var geofence = new Geofence();
        geofence.setName(name);
        long id = storage.addObject(geofence, all);
        geofence.setId(id);
        if (visible) {
            storage.addPermission(new Permission(User.class, user, Geofence.class, id));
        }
        return id;
    }

    @Test
    void stopsUseOnlyRecordedAndVisibleGeofences() throws Exception {
        long depot = addGeofence("Depot", true);
        long customer = addGeofence("Customer", true);
        long restricted = addGeofence("Restricted", false);
        var geofences = StopGeofences.forUser(storage, user);
        var stop = new StopReportItem();
        stop.setGeofenceIds(List.of(restricted, customer, depot, depot, 999L));
        geofences.applyToStop(stop);
        assertEquals(List.of(depot, customer), stop.getGeofenceIds());
        assertEquals("Depot, Customer", stop.getGeofenceNames());

        stop.setGeofenceIds(null);
        geofences.applyToStop(stop);
        assertEquals(List.of(), stop.getGeofenceIds());
        assertEquals("", stop.getGeofenceNames());
    }

    @Test
    void eventPositionQueryMatchesTheDeviceTypeAndTime() throws Exception {
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:stop_positions");
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE tc_positions(id BIGINT, deviceId BIGINT, geofenceIds VARCHAR)");
            statement.execute("CREATE TABLE tc_events(type VARCHAR, eventTime TIMESTAMP, positionId BIGINT, deviceId BIGINT)");
            statement.execute("INSERT INTO tc_positions VALUES (10, 1, '[9,12]'), (11, 1, '[8]'), (12, 2, '[7]'), (13, 1, '[10]')");
            statement.execute("INSERT INTO tc_events VALUES "
                    + "('deviceStopped', TIMESTAMP '2026-01-02 12:00:00', 10, 1), "
                    + "('deviceMoving', TIMESTAMP '2026-01-02 12:00:00', 11, 1), "
                    + "('deviceStopped', TIMESTAMP '2026-01-02 12:00:00', 12, 2), "
                    + "('deviceStopped', TIMESTAMP '2026-01-05 12:00:00', 13, 1)");
            var database = new DatabaseStorage(new Config(), source, new ObjectMapper());
            var provider = new EventsReportProvider(null, null, database);
            var positions = provider.getStopPositions(1,
                    Date.from(Instant.parse("2026-01-01T00:00:00Z")), Date.from(Instant.parse("2026-01-04T00:00:00Z")));
            assertEquals(Set.of(10L), positions.keySet());
            assertEquals(List.of(9L, 12L), positions.get(10L).getGeofenceIds());
        }
    }

    @Test
    void eventReportRetainsTheStopForUsersWithDifferentGeofenceAccess() throws Exception {
        var device = new Device();
        long deviceId = storage.addObject(device, all);
        device.setId(deviceId);
        long anotherUser = user + 1;
        storage.addPermission(new Permission(User.class, user, Device.class, deviceId));
        storage.addPermission(new Permission(User.class, anotherUser, Device.class, deviceId));
        long depot = addGeofence("Depot", true);
        long customer = addGeofence("Customer", true);
        storage.addPermission(new Permission(User.class, anotherUser, Geofence.class, depot));
        var position = new Position();
        position.setDeviceId(deviceId);
        position.setTime(new Date(2000));
        position.setGeofenceIds(List.of(depot, customer));
        position.setId(storage.addObject(position, all));
        var stop = new Event(Event.TYPE_DEVICE_STOPPED, position);
        storage.addObject(stop, all);
        var moving = new Event(Event.TYPE_DEVICE_MOVING, position);
        storage.addObject(moving, all);

        var provider = new EventsReportProvider(mock(Config.class), mock(ReportUtils.class), storage);
        for (long reader : List.of(user, anotherUser, anotherUser + 1)) {
            if (reader == anotherUser + 1) {
                storage.addPermission(new Permission(User.class, reader, Device.class, deviceId));
            }
            var events = provider.getObjects(reader, List.of(), List.of(), List.of(), List.of(),
                    new Date(1000), new Date(3000)).toList();
            assertEquals(2, events.size());
            var readStop = events.stream()
                    .filter(e -> Event.TYPE_DEVICE_STOPPED.equals(e.getType())).findFirst().orElseThrow();
            if (reader == user) {
                assertEquals(List.of(depot, customer), readStop.getAttributes().get("geofenceIds"));
                assertEquals("Depot, Customer", readStop.getString("geofenceNames"));
                assertEquals(0, readStop.getGeofenceId());
            } else if (reader == anotherUser) {
                assertEquals(List.of(depot), readStop.getAttributes().get("geofenceIds"));
                assertEquals(depot, readStop.getGeofenceId());
            } else {
                assertEquals(0, readStop.getGeofenceId());
                assertFalse(readStop.hasAttribute("geofenceIds"));
            }
        }
        assertFalse(moving.hasAttribute("geofenceIds"));
        assertFalse(stop.hasAttribute("geofenceIds"));
    }
}
