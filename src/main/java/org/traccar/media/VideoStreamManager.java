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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class VideoStreamManager {

    private static final int MAX_SEGMENTS = 5;

    private final Map<String, DeviceStream> streams = new ConcurrentHashMap<>();

    @Inject
    public VideoStreamManager() {
    }

    public void handleFrame(String uniqueId, int channel, ByteBuf nalData, long timestamp, boolean isKeyFrame) {
        String key = uniqueId + "_" + channel;
        DeviceStream stream = streams.computeIfAbsent(key, k -> new DeviceStream());
        stream.addFrame(nalData, timestamp, isKeyFrame);
    }

    public String getPlaylist(String uniqueId, int channel) {
        DeviceStream stream = streams.get(uniqueId + "_" + channel);
        return stream != null ? stream.getPlaylist() : DeviceStream.EMPTY_PLAYLIST;
    }

    public void removeStream(String uniqueId, int channel) {
        DeviceStream stream = streams.remove(uniqueId + "_" + channel);
        if (stream != null) {
            stream.release();
        }
    }

    public ByteBuf getSegment(String uniqueId, int channel, int index) {
        DeviceStream stream = streams.get(uniqueId + "_" + channel);
        return stream != null ? stream.getSegment(index) : null;
    }

    static class DeviceStream {

        private final VideoStreamWriter writer = new VideoStreamWriter();
        private final LinkedHashMap<Integer, ByteBuf> segments = new LinkedHashMap<>();
        private ByteBuf currentSegment;
        private int segmentIndex;
        private long firstTimestamp;

        synchronized void addFrame(ByteBuf nalData, long timestamp, boolean isKeyFrame) {
            if (isKeyFrame && currentSegment != null) {
                finalizeSegment();
            }

            if (currentSegment == null) {
                currentSegment = Unpooled.buffer();
                if (firstTimestamp == 0) {
                    firstTimestamp = timestamp;
                }
            }

            writer.write(currentSegment, nalData, timestamp - firstTimestamp, isKeyFrame);
        }

        private void finalizeSegment() {
            segments.put(segmentIndex++, currentSegment);
            currentSegment = null;

            while (segments.size() > MAX_SEGMENTS) {
                Integer oldest = segments.keySet().iterator().next();
                segments.remove(oldest).release();
            }
        }

        synchronized void release() {
            if (currentSegment != null) {
                currentSegment.release();
            }
            for (ByteBuf segment : segments.values()) {
                segment.release();
            }
        }

        static final String EMPTY_PLAYLIST =
                "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:5\n#EXT-X-MEDIA-SEQUENCE:0\n";

        synchronized String getPlaylist() {
            if (currentSegment != null) {
                finalizeSegment();
            }
            if (segments.isEmpty()) {
                return EMPTY_PLAYLIST;
            }

            int firstIndex = segments.keySet().iterator().next();

            StringBuilder sb = new StringBuilder();
            sb.append("#EXTM3U\n");
            sb.append("#EXT-X-VERSION:3\n");
            sb.append("#EXT-X-TARGETDURATION:5\n");
            sb.append("#EXT-X-MEDIA-SEQUENCE:").append(firstIndex).append("\n");

            for (int key : segments.keySet()) {
                sb.append("#EXTINF:3.0,\n");
                sb.append(key).append(".ts\n");
            }

            return sb.toString();
        }

        synchronized ByteBuf getSegment(int index) {
            return segments.get(index);
        }
    }

}
