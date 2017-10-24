/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Log;
import org.traccar.model.Command;

public class MiniFinderProtocolEncoder extends StringProtocolEncoder implements StringProtocolEncoder.ValueFormatter {

    @Override
    public String formatValue(String key, Object value) {

        if (key.equals(Command.KEY_ENABLE)) {
            return (Boolean) value ? "1" : "0";
        } else if (key.equals(Command.KEY_TIMEZONE)) {
            return String.format("%+03d", TimeZone.getTimeZone((String) value).getRawOffset() / 3600000);
        } else if (key.equals(Command.KEY_INDEX)) {
            switch (((Number) value).intValue()) {
                case 0:
                    return "A";
                case 1:
                    return "B";
                case 2:
                    return "C";
                default:
                    return null;
            }
        }

        return null;
    }

    @Override
    protected Object encodeCommand(Command command) {

        initDevicePassword(command, "123456");

        switch (command.getType()) {
            case Command.TYPE_SET_TIMEZONE:
                return formatCommand(command, "{%s}L{%s}", this, Command.KEY_DEVICE_PASSWORD, Command.KEY_TIMEZONE);
            case Command.TYPE_VOICE_MONITORING:
                return formatCommand(command, "{%s}P{%s}", this, Command.KEY_DEVICE_PASSWORD, Command.KEY_ENABLE);
            case Command.TYPE_ALARM_SPEED:
                return formatCommand(command, "{%s}J1{%s}", Command.KEY_DEVICE_PASSWORD, Command.KEY_DATA);
            case Command.TYPE_ALARM_GEOFENCE:
                return formatCommand(command, "{%s}R1{%s}", Command.KEY_DEVICE_PASSWORD, Command.KEY_RADIUS);
            case Command.TYPE_ALARM_VIBRATION:
                return formatCommand(command, "{%s}W1,{%s}", Command.KEY_DEVICE_PASSWORD, Command.KEY_DATA);
            case Command.TYPE_SET_AGPS:
                return formatCommand(command, "{%s}AGPS{%s}", this, Command.KEY_DEVICE_PASSWORD, Command.KEY_ENABLE);
            case Command.TYPE_ALARM_FALL:
                return formatCommand(command, "{%s}F{%s}", this, Command.KEY_DEVICE_PASSWORD, Command.KEY_ENABLE);
            case Command.TYPE_MODE_POWER_SAVING:
                return formatCommand(command, "{%s}SP{%s}", this, Command.KEY_DEVICE_PASSWORD, Command.KEY_ENABLE);
            case Command.TYPE_MODE_DEEP_SLEEP:
                return formatCommand(command, "{%s}DS{%s}", this, Command.KEY_DEVICE_PASSWORD, Command.KEY_ENABLE);
            case Command.TYPE_SOS_NUMBER:
                return formatCommand(command, "{%s}{%s}1,{%s}", this,
                        Command.KEY_DEVICE_PASSWORD, Command.KEY_INDEX, Command.KEY_PHONE);
            case Command.TYPE_SET_INDICATOR:
                return formatCommand(command, "{%s}LED{%s}", Command.KEY_DEVICE_PASSWORD, Command.KEY_DATA);
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                return null;
        }
    }

}
