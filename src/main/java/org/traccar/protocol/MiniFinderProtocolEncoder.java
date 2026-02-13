/*
 * Copyright 2016 - 2019 Anton Tananaev (anton@traccar.org)
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

import java.util.TimeZone;

import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;
import org.traccar.Protocol;

public class MiniFinderProtocolEncoder extends StringProtocolEncoder implements StringProtocolEncoder.ValueFormatter {

    public MiniFinderProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    public String formatValue(String key, Object value) {
        return switch (key) {
            case Command.KEY_ENABLE -> (Boolean) value ? "1" : "0";
            case Command.KEY_TIMEZONE -> String.format(
                    "%+03d", TimeZone.getTimeZone((String) value).getRawOffset() / 3600000);
            case Command.KEY_INDEX -> switch (((Number) value).intValue()) {
                case 0 -> "A";
                case 1 -> "B";
                case 2 -> "C";
                default -> null;
            };
            default -> null;
        };
    }

    @Override
    protected Object encodeCommand(Command command) {

        initDevicePassword(command, "123456");

        return switch (command.getType()) {
            case Command.TYPE_SET_TIMEZONE -> formatCommand(
                    command, "%sL%s", this, Command.KEY_DEVICE_PASSWORD, Command.KEY_TIMEZONE);
            case Command.TYPE_VOICE_MONITORING -> formatCommand(
                    command, "%sP%s", this, Command.KEY_DEVICE_PASSWORD, Command.KEY_ENABLE);
            case Command.TYPE_ALARM_SPEED -> formatCommand(
                    command, "%sJ1%s", Command.KEY_DEVICE_PASSWORD, Command.KEY_DATA);
            case Command.TYPE_ALARM_GEOFENCE -> formatCommand(
                    command, "%sR1%s", Command.KEY_DEVICE_PASSWORD, Command.KEY_RADIUS);
            case Command.TYPE_ALARM_VIBRATION -> formatCommand(
                    command, "%sW1,%s", Command.KEY_DEVICE_PASSWORD, Command.KEY_DATA);
            case Command.TYPE_SET_AGPS -> formatCommand(
                    command, "%sAGPS%s", this, Command.KEY_DEVICE_PASSWORD, Command.KEY_ENABLE);
            case Command.TYPE_ALARM_FALL -> formatCommand(
                    command, "%sF%s", this, Command.KEY_DEVICE_PASSWORD, Command.KEY_ENABLE);
            case Command.TYPE_MODE_POWER_SAVING -> formatCommand(
                    command, "%sSP%s", this, Command.KEY_DEVICE_PASSWORD, Command.KEY_ENABLE);
            case Command.TYPE_MODE_DEEP_SLEEP -> formatCommand(
                    command, "%sDS%s", this, Command.KEY_DEVICE_PASSWORD, Command.KEY_ENABLE);
            case Command.TYPE_SOS_NUMBER -> formatCommand(
                    command, "%s%s1,%s", this, Command.KEY_DEVICE_PASSWORD, Command.KEY_INDEX, Command.KEY_PHONE);
            case Command.TYPE_SET_INDICATOR -> formatCommand(
                    command, "%sLED%s", Command.KEY_DEVICE_PASSWORD, Command.KEY_DATA);
            default -> null;
        };
    }

}
