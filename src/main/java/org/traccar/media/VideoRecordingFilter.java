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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record VideoRecordingFilter(
        String from, String to, int channel, int resourceType, int streamType, int storageType, String alarmMask) {

    private static final DateTimeFormatter BCD_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    public void validate() {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Recording interval is required");
        }
        Instant start = Instant.parse(from);
        Instant end = Instant.parse(to);
        int firstYear = start.atZone(ZoneId.of("UTC")).getYear();
        int lastYear = end.atZone(ZoneId.of("UTC")).getYear();
        if (start.isAfter(end) || Duration.between(start, end).compareTo(Duration.ofDays(31)) > 0
                || start.getNano() != 0 || end.getNano() != 0 || firstYear < 2000 || lastYear > 2099) {
            throw new IllegalArgumentException("Invalid recording interval (maximum 31 days, whole seconds)");
        }
        if (channel < 0 || channel > 31 || resourceType < 0 || resourceType > 3
                || streamType < 0 || streamType > 2 || storageType < 0 || storageType > 2
                || alarmMask != null && !alarmMask.matches("[0-9a-fA-F]{16}")) {
            throw new IllegalArgumentException("Invalid recording filter");
        }
    }

    public String encodeTime(String value, ZoneId zone) {
        var local = Instant.parse(value).atZone(zone);
        if (local.getYear() < 2000 || local.getYear() > 2099) {
            throw new IllegalArgumentException("Recording date outside JT808 BCD range");
        }
        return BCD_TIME.format(local);
    }

    public String mask() {
        return alarmMask != null ? alarmMask : "0000000000000000";
    }
}
