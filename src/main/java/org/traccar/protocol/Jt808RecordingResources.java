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
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import org.traccar.media.VideoRecording;
import org.traccar.media.VideoRecordingManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

final class Jt808RecordingResources {

    private Jt808RecordingResources() {
    }

    private static String readTime(ByteBuf data, ZoneId zone) {
        int[] values = new int[6];
        for (int i = 0; i < values.length; i++) {
            int value = data.readUnsignedByte();
            if ((value >>> 4) > 9 || (value & 0x0f) > 9) {
                throw new IllegalArgumentException("Invalid recording BCD time");
            }
            values[i] = (value >>> 4) * 10 + (value & 0x0f);
        }
        var local = LocalDateTime.of(
                2000 + values[0], values[1], values[2], values[3], values[4], values[5]);
        var offsets = zone.getRules().getValidOffsets(local);
        if (offsets.size() != 1) {
            throw new IllegalArgumentException("Ambiguous or nonexistent recording local time");
        }
        return local.toInstant(offsets.getFirst()).toString();
    }

    static List<VideoRecording> decode(ByteBuf data, ZoneId zone) {
        if (data.readableBytes() < 6) {
            throw new IllegalArgumentException("Truncated recording response");
        }
        data.readUnsignedShort();
        long count = data.readUnsignedInt();
        if (count > VideoRecordingManager.MAX_RECORDINGS || data.readableBytes() != count * 28) {
            throw new IllegalArgumentException("Invalid recording count or body length");
        }
        var result = new ArrayList<VideoRecording>((int) count);
        for (int i = 0; i < count; i++) {
            int channel = data.readUnsignedByte();
            String from = readTime(data, zone);
            String to = readTime(data, zone);
            String alarmMask = String.format("%016x", data.readLong());
            int resourceType = data.readUnsignedByte();
            int streamType = data.readUnsignedByte();
            int storageType = data.readUnsignedByte();
            long size = data.readUnsignedInt();
            if (channel < 1 || channel > 31 || from.compareTo(to) > 0
                    || resourceType > 2 || streamType < 1 || streamType > 2
                    || storageType < 1 || storageType > 2) {
                throw new IllegalArgumentException("Invalid recording resource");
            }
            result.add(new VideoRecording(channel, from, to, alarmMask, resourceType, streamType, storageType, size));
        }
        return List.copyOf(result);
    }
}
