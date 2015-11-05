/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.traccar.model.Event;

import java.util.AbstractMap;
import java.util.Map;

public class ObdDecoder {

    private static final int MODE_CURRENT = 0x01;
    private static final int MODE_FREEZE_FRAME = 0x02;

    private static final int PID_ENGINE_LOAD = 0x04;
    private static final int PID_COOLANT_TEMPERATURE = 0x05;
    private static final int PID_ENGINE_RPM = 0x0C;
    private static final int PID_VEHICLE_SPEED = 0x0D;
    private static final int PID_THROTTLE_POSITION = 0x11;
    private static final int PID_MIL_DISTANCE = 0x21;
    private static final int PID_FUEL_LEVEL = 0x2F;
    private static final int PID_DISTANCE_CLEARED = 0x31;

    public static Map.Entry<String, Object> decode(int mode, int pid, String value) {
        switch (mode) {
            case MODE_CURRENT:
            case MODE_FREEZE_FRAME:
                return decodeData(pid, value);
            default:
                return null;
        }
    }

    private static Map.Entry<String, Object> createEntry(String key, Object value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    private static Map.Entry<String, Object> decodeData(int pid, String value) {
        int intValue = Integer.parseInt(value, 16);
        switch (pid) {
            case PID_ENGINE_LOAD:
                return createEntry("engine-load", intValue * 100 / 255);
            case PID_COOLANT_TEMPERATURE:
                return createEntry("coolant-temperature", intValue - 40);
            case PID_ENGINE_RPM:
                return createEntry(Event.KEY_RPM, intValue / 4);
            case PID_VEHICLE_SPEED:
                return createEntry(Event.KEY_OBD_SPEED, intValue);
            case PID_THROTTLE_POSITION:
                return createEntry("throttle", intValue * 100 / 255);
            case PID_MIL_DISTANCE:
                return createEntry("mil-distance", intValue);
            case PID_FUEL_LEVEL:
                return createEntry(Event.KEY_FUEL, intValue * 100 / 255);
            case PID_DISTANCE_CLEARED:
                return createEntry(Event.KEY_FUEL, intValue);
            default:
                return null;
        }
    }

}
