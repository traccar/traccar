package org.traccar.session.cache;

import org.junit.jupiter.api.Test;
import org.traccar.broadcast.BroadcastService;
import org.traccar.config.Config;
import org.traccar.model.Device;
import org.traccar.model.Geofence;
import org.traccar.model.Notification;
import org.traccar.model.Permission;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Request;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CacheManagerTest {

    @Test
    public void testReplacePositionWithSameFixTime() throws Exception {

        Storage storage = mock(Storage.class);
        BroadcastService broadcastService = mock(BroadcastService.class);
        Device device = new Device();
        device.setId(1);
        when(storage.getObject(eq(Server.class), any(Request.class))).thenReturn(new Server());
        when(storage.getObject(eq(Device.class), any(Request.class))).thenReturn(device);
        doNothing().when(broadcastService).registerListener(any());

        CacheManager cacheManager = new CacheManager(new Config(), storage, broadcastService);

        long deviceId = 1;
        Object key = new Object();
        cacheManager.addDevice(deviceId, key);

        long fixTime = System.currentTimeMillis();

        Position first = new Position();
        first.setDeviceId(deviceId);
        first.setFixTime(new Date(fixTime));

        Position replacement = new Position();
        replacement.setDeviceId(deviceId);
        replacement.setFixTime(new Date(fixTime));

        cacheManager.updatePosition(first);
        cacheManager.updatePosition(replacement);

        assertSame(replacement, cacheManager.getPosition(deviceId));
    }

    @Test
    public void testAddDeviceLoadsOnlyItsOwnPermissions() throws Exception {

        Storage storage = mock(Storage.class);
        BroadcastService broadcastService = mock(BroadcastService.class);
        Device device = new Device();
        device.setId(1);
        User user = new User();
        user.setId(2);
        when(storage.getObject(eq(Server.class), any(Request.class))).thenReturn(new Server());
        when(storage.getObject(eq(Device.class), any(Request.class))).thenReturn(device);
        when(storage.getObject(eq(User.class), any(Request.class))).thenReturn(user);
        when(storage.getPermissions(User.class, 0, Device.class, 1))
                .thenReturn(List.of(new Permission(User.class, 2, Device.class, 1)));
        doNothing().when(broadcastService).registerListener(any());

        CacheManager cacheManager = new CacheManager(new Config(), storage, broadcastService);
        cacheManager.addDevice(1, new Object());

        assertEquals(Set.of(user), cacheManager.getDeviceObjects(1, User.class));
        verify(storage).getPermissions(User.class, 0, Device.class, 1);
        verify(storage).getPermissions(User.class, 2, Notification.class, 0);
        verify(storage).getPermissions(Device.class, 1, Geofence.class, 0);
        verify(storage, never()).getPermissions(any(), eq(0L), any(), eq(0L));
    }

}
