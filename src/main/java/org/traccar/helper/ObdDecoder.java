/*
 * Copyright 2015 - 2020 Anton Tananaev (anton@traccar.org)
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
package org.traccar.helper;

import org.traccar.model.Position;

import java.util.AbstractMap;
import java.util.Map;

public final class ObdDecoder {

    private ObdDecoder() {
    }

    private static final int MODE_CURRENT = 0x01;
    private static final int MODE_FREEZE_FRAME = 0x02;
    private static final int MODE_CODES = 0x03;

    public static Map.Entry<String, Object> decode(int mode, String value) {
        return switch (mode) {
            case MODE_CURRENT, MODE_FREEZE_FRAME -> decodeData(
                    Integer.parseInt(value.substring(0, 2), 16),
                    Long.parseLong(value.substring(2), 16), true);
            case MODE_CODES -> decodeCodes(value);
            default -> null;
        };
    }

    private static Map.Entry<String, Object> createEntry(String key, Object value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public static Map.Entry<String, Object> decodeCodes(String value) {
        StringBuilder codes = new StringBuilder();
        for (int i = 0; i < value.length() / 4; i++) {
            int numValue = Integer.parseInt(value.substring(i * 4, (i + 1) * 4), 16);
            codes.append(' ').append(decodeCode(numValue));
        }
        if (!codes.isEmpty()) {
            return createEntry(Position.KEY_DTCS, codes.toString().trim());
        } else {
            return null;
        }
    }

    public static String decodeCode(int value) {
        char prefix = switch (value >> 14) {
            case 1 -> 'C';
            case 2 -> 'B';
            case 3 -> 'U';
            default -> 'P';
        };
        return String.format("%c%04X", prefix, value & 0x3FFF);
    }

    public static Map.Entry<String, Object> decodeData(int pid, long value, boolean convert) {
        return switch (pid) {
            case 0x04 -> createEntry(Position.KEY_ENGINE_LOAD, convert ? value * 100 / 255 : value);
            case 0x05 -> createEntry(Position.KEY_COOLANT_TEMP, convert ? value - 40 : value);
            case 0x0B -> createEntry("mapIntake", value);
            case 0x0C -> createEntry(Position.KEY_RPM, convert ? value / 4 : value);
            case 0x0D -> createEntry(Position.KEY_OBD_SPEED, value);
            case 0x0F -> createEntry("intakeTemp", convert ? value - 40 : value);
            case 0x11 -> createEntry(Position.KEY_THROTTLE, convert ? value * 100 / 255 : value);
            case 0x21 -> createEntry("milDistance", value);
            case 0x2F -> createEntry(Position.KEY_FUEL_LEVEL, convert ? value * 100 / 255 : value);
            case 0x31 -> createEntry("clearedDistance", value);
            default -> null;
        };
    }

}
