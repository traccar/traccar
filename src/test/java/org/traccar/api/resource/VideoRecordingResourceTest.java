package org.traccar.api.resource;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.traccar.ProtocolTest;
import org.traccar.api.security.PermissionsService;
import org.traccar.helper.LogAction;
import org.traccar.media.VideoRecordingFilter;
import org.traccar.media.VideoRecordingManager;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.protocol.Jt808ProtocolDecoder;
import org.traccar.session.ConnectionManager;
import org.traccar.session.DeviceSession;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class VideoRecordingResourceTest extends ProtocolTest {

    private static class Resource extends VideoRecordingResource {
        private final long userId;

        Resource(VideoRecordingManager manager, ConnectionManager connections, PermissionsService permissions,
                LogAction logger, long userId) {
            super(manager, connections, logger);
            this.permissionsService = permissions;
            this.userId = userId;
        }

        @Override
        protected long getUserId() {
            return userId;
        }
    }

    private VideoRecordingFilter filter() {
        return new VideoRecordingFilter(
                "2026-10-22T12:00:00Z", "2026-10-23T01:00:00Z", 0, 0, 0, 0, null);
    }

    private DeviceSession connect(ConnectionManager connections, EmbeddedChannel channel) {
        var session = mock(DeviceSession.class);
        when(session.getChannel()).thenReturn(channel);
        when(session.supportsLiveCommands()).thenReturn(true);
        when(connections.getDeviceSession(1)).thenReturn(session);
        return session;
    }

    @Test
    public void testApiToProtocolResponseWithoutPositionWrites() throws Exception {
        var manager = new VideoRecordingManager();
        var connections = mock(ConnectionManager.class);
        var permissions = mock(PermissionsService.class);
        var logger = mock(LogAction.class);
        var resource = new Resource(manager, connections, permissions, logger, 10);
        var decoder = inject(new Jt808ProtocolDecoder(null));
        decoder.setRecordingManager(manager);
        var session = connect(connections, channel(decoder));
        var response = resource.create(1, filter());
        assertEquals(202, response.getStatus());
        var pending = (VideoRecordingManager.Result) response.getEntity();
        assertEquals("pending", pending.state());
        var command = ArgumentCaptor.forClass(Command.class);
        verify(session).sendCommand(command.capture());
        assertEquals("videoRecordingList", command.getValue().getType());
        assertEquals(1, command.getValue().getInteger("sequence"));
        assertEquals(true, command.getValue().getBoolean(Command.KEY_NO_QUEUE));
        verifyDecode(decoder, binary("7e1205003e0b3a73ce2ff23456000100000002012610222000002610222005008000000000000001020101f0000000032610230800002610230810000000000000000000000202000001007c7e"));
        var result = resource.get(1, pending.requestId());
        assertEquals("completed", result.state());
        assertEquals(2, result.recordings().size());
        var otherUser = new Resource(manager, connections, permissions, logger, 11);
        assertThrows(NotFoundException.class, () -> otherUser.get(1, pending.requestId()));
        assertThrows(NotFoundException.class, () -> resource.get(2, pending.requestId()));
        verify(logger).command(any(), eq(10L), eq(0L), eq(1L), eq("videoRecordingList"));
    }

    @Test
    public void testPermissionsCheckedBeforeSendingAndPolling() throws Exception {
        var manager = new VideoRecordingManager();
        var connections = mock(ConnectionManager.class);
        var permissions = mock(PermissionsService.class);
        var resource = new Resource(manager, connections, permissions, mock(LogAction.class), 10);
        doThrow(new SecurityException()).when(permissions).checkPermission(Device.class, 10, 1);
        assertThrows(SecurityException.class, () -> resource.create(1, filter()));
        assertThrows(SecurityException.class, () -> resource.get(1, UUID.randomUUID()));
        verifyNoInteractions(connections);
    }

    @Test
    public void testRestrictedCommandsCannotStartQuery() throws Exception {
        var connections = mock(ConnectionManager.class);
        var permissions = mock(PermissionsService.class);
        var resource = new Resource(
                new VideoRecordingManager(), connections, permissions, mock(LogAction.class), 10);
        doThrow(new SecurityException()).when(permissions).checkRestriction(eq(10L), any());
        assertThrows(SecurityException.class, () -> resource.create(1, filter()));
        verifyNoInteractions(connections);
    }

    @Test
    public void testInvalidFilterAndOfflineDevices() {
        var connections = mock(ConnectionManager.class);
        var resource = new Resource(new VideoRecordingManager(), connections,
                mock(PermissionsService.class), mock(LogAction.class), 10);
        assertThrows(BadRequestException.class, () -> resource.create(1, null));
        assertThrows(BadRequestException.class, () -> resource.create(1,
                new VideoRecordingFilter(null, null, 0, 0, 0, 0, null)));
        assertThrows(BadRequestException.class, () -> resource.create(1,
                new VideoRecordingFilter("invalid", "invalid", 0, 0, 0, 0, null)));
        var error = assertThrows(WebApplicationException.class, () -> resource.create(1, filter()));
        assertEquals(409, error.getResponse().getStatus());
    }

    @Test
    public void testSendFailureDoesNotCompleteQuery() {
        var manager = new VideoRecordingManager();
        var connections = mock(ConnectionManager.class);
        var resource = new Resource(manager, connections, mock(PermissionsService.class), mock(LogAction.class), 10);
        var session = connect(connections, channel(new Jt808ProtocolDecoder(null)));
        doThrow(new IllegalStateException("Disconnected")).when(session).sendCommand(any());
        var error = assertThrows(WebApplicationException.class, () -> resource.create(1, filter()));
        assertEquals(502, error.getResponse().getStatus());
        assertNull(manager.get(1, 10, UUID.randomUUID()));
        manager.create(1, 10);
    }
}
