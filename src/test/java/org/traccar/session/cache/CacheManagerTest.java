package org.traccar.session.cache;

import org.junit.jupiter.api.Test;
import org.traccar.config.Config;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.broadcast.BroadcastService;
import org.traccar.model.Server;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Request;

import java.util.List;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
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
        when(storage.getPermissions(eq(Device.class), any())).thenReturn(List.of());
        when(storage.getPermissions(eq(User.class), eq(Device.class))).thenReturn(List.of());
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

}
