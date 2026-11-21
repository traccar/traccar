package org.traccar.session.cache;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.traccar.broadcast.BroadcastService;
import org.traccar.config.Config;
import org.traccar.database.DeviceLookupService;
import org.traccar.model.Device;
import org.traccar.model.Permission;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Request;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
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

        CacheManager cacheManager = new CacheManager(
                new Config(), storage, broadcastService, mock(DeviceLookupService.class));

        long deviceId = 1;
        Object key = new Object();
        cacheManager.addDevice(deviceId, null, key);

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
        Device device = new Device();
        device.setId(1);

        var cache = new CacheManager(
                new Config(), storage, mock(BroadcastService.class), mock(DeviceLookupService.class));
        cache.addDevice(device.getId(), device, new Object());

        ArgumentCaptor<List<Permission>> batch = ArgumentCaptor.captor();
        verify(storage).getPermissions(batch.capture());
        var filters = batch.getValue();
        assertEquals(7, filters.size());
        for (var filter : filters) {
            assertEquals(device.getId(), filter.get().get("deviceId"));
        }
    }

}
