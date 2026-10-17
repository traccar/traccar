package org.traccar.session.cache;

import org.junit.jupiter.api.Test;
import org.traccar.broadcast.BroadcastService;
import org.traccar.config.Config;
import org.traccar.model.Device;
import org.traccar.model.ObjectOperation;
import org.traccar.model.Permission;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Request;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    public void testLinkedCycleAndDeletion() throws Exception {
        Storage storage = mock(Storage.class);
        Device first = new Device();
        first.setId(1);
        Device second = new Device();
        second.setId(2);
        when(storage.getObject(eq(Device.class), any(Request.class))).thenReturn(first, second, first, second);
        when(storage.getPermissions(Device.class, Device.class)).thenReturn(List.of(
                new Permission(Device.class, 1, Device.class, 2),
                new Permission(Device.class, 2, Device.class, 1)));
        CacheManager manager = new CacheManager(new Config(), storage, mock(BroadcastService.class));
        Object firstKey = new Object();
        Object secondKey = new Object();
        manager.addDevice(1, firstKey);
        assertSame(second, manager.getObject(Device.class, 2));
        manager.addDevice(2, secondKey);
        manager.removeDevice(1, firstKey);
        assertSame(first, manager.getObject(Device.class, 1));
        manager.removeDevice(2, secondKey);
        assertNull(manager.getObject(Device.class, 1));
        assertNull(manager.getObject(Device.class, 2));

        manager.addDevice(1, firstKey);
        Position position = new Position();
        position.setDeviceId(1);
        position.setFixTime(new Date());
        manager.updatePosition(position);
        manager.invalidateObject(false, Device.class, 1, ObjectOperation.DELETE);
        manager.updatePosition(position);
        assertNull(manager.getPosition(1));
        assertNull(manager.getObject(Device.class, 2));
    }

    @Test
    public void testInitializationFailureCanBeRetried() throws Exception {
        Storage storage = mock(Storage.class);
        Device device = new Device();
        device.setId(1);
        when(storage.getObject(eq(Device.class), any(Request.class))).thenReturn(device);
        when(storage.getPermissions(User.class, Device.class))
                .thenThrow(new StorageException("Failed to load permissions")).thenReturn(List.of());
        CacheManager manager = new CacheManager(new Config(), storage, mock(BroadcastService.class));
        Object key = new Object();
        assertThrows(StorageException.class, () -> manager.addDevice(1, key));
        assertNull(manager.getObject(Device.class, 1));
        manager.addDevice(1, key);
        assertSame(device, manager.getObject(Device.class, 1));
        manager.removeDevice(1, key);
        assertNull(manager.getObject(Device.class, 1));
    }

    @Test
    public void testUnlinkPreservesExternalReference() throws Exception {
        Storage storage = mock(Storage.class);
        Device first = new Device();
        first.setId(1);
        Device second = new Device();
        second.setId(2);
        when(storage.getObject(eq(Device.class), any(Request.class))).thenReturn(first, second);
        CacheManager manager = new CacheManager(new Config(), storage, mock(BroadcastService.class));
        Object key = new Object();
        manager.addDevice(1, key);
        manager.invalidatePermission(false, Device.class, 1, Device.class, 2, true);
        Position position = new Position();
        position.setDeviceId(2);
        position.setFixTime(new Date());
        manager.updatePosition(position);
        assertSame(position, manager.getPosition(2));
        manager.addDevice(2, key);
        manager.invalidatePermission(false, Device.class, 1, Device.class, 2, false);
        assertSame(second, manager.getObject(Device.class, 2));
        manager.removeDevice(2, key);
        assertNull(manager.getObject(Device.class, 2));
        assertNull(manager.getPosition(2));
        assertSame(first, manager.getObject(Device.class, 1));
    }

    @Test
    public void testLinkedInitializationFailureReleasesParent() throws Exception {
        Storage storage = mock(Storage.class);
        Device first = new Device();
        first.setId(1);
        when(storage.getObject(eq(Device.class), any(Request.class))).thenReturn(first).thenReturn(null);
        when(storage.getPermissions(Device.class, Device.class)).thenReturn(List.of(
                new Permission(Device.class, 1, Device.class, 2)));
        CacheManager manager = new CacheManager(new Config(), storage, mock(BroadcastService.class));
        assertThrows(StorageException.class, () -> manager.addDevice(1, new Object()));
        assertNull(manager.getObject(Device.class, 1));
        assertNull(manager.getObject(Device.class, 2));
    }

    @Test
    public void testPositionLoadFailureReleasesLinkedCycle() throws Exception {
        Storage storage = mock(Storage.class);
        Device first = new Device();
        first.setId(1);
        first.setPositionId(10);
        Device second = new Device();
        second.setId(2);
        when(storage.getObject(eq(Device.class), any(Request.class))).thenReturn(first, second);
        when(storage.getObject(eq(Position.class), any(Request.class)))
                .thenThrow(new StorageException("Failed to load position"));
        when(storage.getPermissions(Device.class, Device.class)).thenReturn(List.of(
                new Permission(Device.class, 1, Device.class, 2),
                new Permission(Device.class, 2, Device.class, 1)));
        CacheManager manager = new CacheManager(new Config(), storage, mock(BroadcastService.class));
        assertThrows(StorageException.class, () -> manager.addDevice(1, new Object()));
        assertNull(manager.getObject(Device.class, 1));
        assertNull(manager.getObject(Device.class, 2));
        assertNull(manager.getPosition(1));
        assertNull(manager.getPosition(2));
    }

    @Test
    public void testSelfLinkAndDuplicateReferenceAreReleased() throws Exception {
        Storage storage = mock(Storage.class);
        Device device = new Device();
        device.setId(1);
        when(storage.getObject(eq(Device.class), any(Request.class))).thenReturn(device);
        when(storage.getPermissions(Device.class, Device.class)).thenReturn(List.of(
                new Permission(Device.class, 1, Device.class, 1)));
        CacheManager manager = new CacheManager(new Config(), storage, mock(BroadcastService.class));
        Object key = new Object();
        manager.addDevice(1, key);
        manager.addDevice(1, key);
        assertSame(device, manager.getObject(Device.class, 1));
        manager.removeDevice(1, key);
        assertNull(manager.getObject(Device.class, 1));
        manager.removeDevice(1, key);
        assertNull(manager.getObject(Device.class, 1));
    }

    @Test
    public void testPromotingGraphNodePreservesLinks() {
        CacheGraph graph = new CacheGraph();
        Device first = new Device();
        first.setId(1);
        Device second = new Device();
        second.setId(2);
        graph.addObject(first);
        graph.addLink(Device.class, 1, Device.class, 2, () -> second);
        graph.addObject(second);
        assertSame(second, graph.getObjects(Device.class, 1, Device.class, Set.of(), true)
                .findFirst().orElseThrow());
        graph.removeObject(Device.class, 2);
        assertTrue(graph.getObjects(Device.class, 1, Device.class, Set.of(), true).findAny().isEmpty());
    }

}
