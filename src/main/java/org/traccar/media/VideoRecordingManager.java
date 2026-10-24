/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.media;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class VideoRecordingManager {

    public static final int MAX_RECORDINGS = 10000;
    private static final int MAX_RESPONSE_BYTES = 6 + 28 * MAX_RECORDINGS;
    private static final int MAX_REQUESTS = 64;
    private static final int MAX_CACHED_RECORDINGS = 20000;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Duration RETENTION = Duration.ofMinutes(5);

    public record Result(
            UUID requestId, long deviceId, String state, List<VideoRecording> recordings, String error) {
    }

    public record Request(UUID id, int sequence) {
    }

    private static class Pending {
        private final UUID id = UUID.randomUUID();
        private final long deviceId;
        private final long userId;
        private final int sequence;
        private final Instant created;
        private final Map<Integer, Parts> responses = new HashMap<>();
        private String state = "pending";
        private String error;
        private List<VideoRecording> recordings = List.of();

        Pending(long deviceId, long userId, int sequence, Instant created) {
            this.deviceId = deviceId;
            this.userId = userId;
            this.sequence = sequence;
            this.created = created;
        }

        Result result() {
            return new Result(id, deviceId, state, recordings, error);
        }

        void finish(String state, String error, List<VideoRecording> recordings) {
            this.state = state;
            this.error = error;
            this.recordings = List.copyOf(recordings);
            responses.clear();
        }
    }

    private static class Parts {
        private final byte[][] packets;
        private int bytes;
        private int received;

        Parts(int count) {
            packets = new byte[count][];
        }

        byte[] add(int index, byte[] data) {
            if (packets[index] != null) {
                if (!Arrays.equals(packets[index], data)) {
                    throw new IllegalArgumentException("Conflicting recording response packet");
                }
                return null;
            }
            if (bytes + data.length > MAX_RESPONSE_BYTES) {
                throw new IllegalArgumentException("Recording response exceeds limit");
            }
            packets[index] = data.clone();
            bytes += data.length;
            received++;
            if (received != packets.length) {
                return null;
            }
            var output = new ByteArrayOutputStream(bytes);
            for (byte[] packet : packets) {
                output.writeBytes(packet);
            }
            return output.toByteArray();
        }
    }

    private final Clock clock;
    private final AtomicInteger serial = new AtomicInteger();
    private final Map<UUID, Pending> requests = new LinkedHashMap<>();

    @Inject
    public VideoRecordingManager() {
        this(Clock.systemUTC());
    }

    VideoRecordingManager(Clock clock) {
        this.clock = clock;
    }

    private void expire() {
        Instant now = clock.instant();
        requests.values().removeIf(query -> !query.created.plus(RETENTION).isAfter(now));
        for (Pending query : requests.values()) {
            if (query.state.equals("pending") && !query.created.plus(TIMEOUT).isAfter(now)) {
                query.finish("expired", "Device response timed out", List.of());
            }
        }
    }

    public synchronized Request create(long deviceId, long userId) {
        expire();
        if (requests.size() >= MAX_REQUESTS) {
            throw new IllegalStateException("Recording query capacity reached");
        }
        if (requests.values().stream().anyMatch(
                query -> query.deviceId == deviceId && query.state.equals("pending"))) {
            throw new IllegalStateException("A recording query is already pending for this device");
        }
        int sequence;
        do {
            sequence = serial.updateAndGet(previous -> previous % 0xffff + 1);
        } while (isRetained(deviceId, sequence));
        var query = new Pending(deviceId, userId, sequence, clock.instant());
        requests.put(query.id, query);
        return new Request(query.id, sequence);
    }

    private boolean isRetained(long deviceId, int sequence) {
        return requests.values().stream().anyMatch(
                query -> query.deviceId == deviceId && query.sequence == sequence);
    }

    private Pending pending(long deviceId) {
        return requests.values().stream().filter(
                query -> query.deviceId == deviceId && query.state.equals("pending")).findFirst().orElse(null);
    }

    public synchronized Result get(long deviceId, long userId, UUID id) {
        expire();
        Pending query = requests.get(id);
        return query != null && query.deviceId == deviceId && query.userId == userId ? query.result() : null;
    }

    public synchronized byte[] receive(
            long deviceId, int responseSequence, int count, int index, byte[] data) {
        expire();
        Pending query = pending(deviceId);
        if (query == null) {
            return null;
        }
        if (count < 1 || count > 1024 || index < 1 || index > count || data.length > 1023) {
            throw new IllegalArgumentException("Invalid recording response packet");
        }
        if (index == 1 && data.length >= 2
                && ((data[0] & 0xff) << 8 | data[1] & 0xff) != query.sequence) {
            query.responses.remove(responseSequence);
            return null;
        }
        Parts parts = query.responses.get(responseSequence);
        if (parts == null) {
            if (query.responses.size() >= 2) {
                throw new IllegalArgumentException("Too many recording response groups");
            }
            parts = new Parts(count);
            query.responses.put(responseSequence, parts);
        } else if (parts.packets.length != count) {
            throw new IllegalArgumentException("Recording response packet count changed");
        }
        byte[] body = parts.add(index - 1, data);
        if (body != null) {
            query.responses.remove(responseSequence);
            if (body.length < 2 || ((body[0] & 0xff) << 8 | body[1] & 0xff) != query.sequence) {
                return null;
            }
        }
        return body;
    }

    public synchronized void complete(long deviceId, int sequence, List<VideoRecording> recordings) {
        expire();
        Pending query = pending(deviceId);
        if (query != null && query.sequence == sequence) {
            int cached = requests.values().stream().mapToInt(value -> value.recordings.size()).sum();
            if (recordings.size() > MAX_RECORDINGS || cached + recordings.size() > MAX_CACHED_RECORDINGS) {
                query.finish("failed", "Recording result capacity reached", List.of());
            } else {
                query.finish("completed", null, recordings);
            }
        }
    }

    public synchronized void fail(long deviceId, int sequence, String error) {
        expire();
        Pending query = pending(deviceId);
        if (query != null && query.sequence == sequence) {
            query.finish("failed", error, List.of());
        }
    }
}
