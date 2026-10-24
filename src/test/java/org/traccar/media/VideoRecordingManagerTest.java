package org.traccar.media;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VideoRecordingManagerTest {

    @Test
    public void testIsolationCorrelationAndOverlappingRequests() {
        var manager = new VideoRecordingManager();
        var request = manager.create(1, 10);
        assertNull(manager.get(1, 11, request.id()));
        assertNull(manager.get(2, 10, request.id()));
        assertThrows(IllegalStateException.class, () -> manager.create(1, 11));
        manager.complete(2, request.sequence(), List.of());
        manager.complete(1, request.sequence() + 1, List.of());
        assertEquals("pending", manager.get(1, 10, request.id()).state());
        manager.complete(1, request.sequence(), List.of());
        assertEquals("completed", manager.get(1, 10, request.id()).state());
        var next = manager.create(1, 11);
        manager.complete(1, request.sequence(), List.of());
        assertEquals("pending", manager.get(1, 11, next.id()).state());
    }

    @Test
    public void testTimeoutAndRetentionReleasePendingBuffers() {
        var clock = mock(Clock.class);
        Instant now = Instant.parse("2026-10-24T00:00:00Z");
        when(clock.instant()).thenReturn(now);
        var manager = new VideoRecordingManager(clock);
        var request = manager.create(1, 10);
        manager.receive(1, 3, 2, 2, new byte[100]);
        when(clock.instant()).thenReturn(now.plusSeconds(30));
        assertEquals("expired", manager.get(1, 10, request.id()).state());
        assertNull(manager.receive(1, 3, 2, 1, new byte[] {0, 1}));
        manager.create(1, 10);
        when(clock.instant()).thenReturn(now.plusSeconds(301));
        assertNull(manager.get(1, 10, request.id()));
    }

    @Test
    public void testCapacityAndMalformedFragmentsAreBounded() {
        var manager = new VideoRecordingManager();
        var request = manager.create(1, 10);
        assertThrows(IllegalArgumentException.class, () -> manager.receive(1, 1, 1025, 1, new byte[1]));
        assertThrows(IllegalArgumentException.class, () -> manager.receive(1, 1, 2, 3, new byte[1]));
        assertThrows(IllegalArgumentException.class, () -> manager.receive(1, 1, 1, 1, new byte[1024]));
        manager.receive(1, 1, 2, 2, new byte[] {1});
        assertThrows(IllegalArgumentException.class, () -> manager.receive(1, 1, 2, 2, new byte[] {2}));
        assertEquals("pending", manager.get(1, 10, request.id()).state());
        for (int i = 2; i <= 64; i++) {
            manager.create(i, 10);
        }
        assertThrows(IllegalStateException.class, () -> manager.create(65, 10));
    }

    @Test
    public void testCompletedRecordingCacheIsBounded() {
        var manager = new VideoRecordingManager();
        var recording = new VideoRecording(1, "2026-10-22T12:00:00Z", "2026-10-22T12:01:00Z",
                "0000000000000000", 2, 1, 1, 100);
        var recordings = java.util.Collections.nCopies(VideoRecordingManager.MAX_RECORDINGS, recording);
        for (int deviceId = 1; deviceId <= 2; deviceId++) {
            var request = manager.create(deviceId, 10);
            manager.complete(deviceId, request.sequence(), recordings);
            assertEquals("completed", manager.get(deviceId, 10, request.id()).state());
        }
        var overflow = manager.create(3, 10);
        manager.complete(3, overflow.sequence(), List.of(recording));
        assertEquals("failed", manager.get(3, 10, overflow.id()).state());
    }

    @Test
    public void testFilterValidationAndZoneConversion() {
        var filter = new VideoRecordingFilter(
                "2026-10-22T12:00:00Z", "2026-10-22T13:00:00Z", 0, 0, 0, 0, null);
        filter.validate();
        assertEquals("261022090000", filter.encodeTime(filter.from(), ZoneId.of("America/Santiago")));
        assertThrows(IllegalArgumentException.class,
                () -> new VideoRecordingFilter(null, null, 0, 0, 0, 0, null).validate());
        assertThrows(IllegalArgumentException.class,
                () -> new VideoRecordingFilter(filter.to(), filter.from(), 0, 0, 0, 0, null).validate());
        assertThrows(IllegalArgumentException.class,
                () -> new VideoRecordingFilter(filter.from(), filter.to(), 0, 0, 0, 0, "-1").validate());
        assertThrows(IllegalArgumentException.class,
                () -> filter.encodeTime("2100-01-01T00:00:00Z", ZoneId.of("UTC")));
    }
}
