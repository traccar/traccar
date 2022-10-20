/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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

import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;
import org.traccar.Protocol;

public class Gps103ProtocolEncoder extends StringProtocolEncoder implements StringProtocolEncoder.ValueFormatter {

    public Gps103ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    public String formatValue(String key, Object value) {

        if (key.equals(Command.KEY_FREQUENCY)) {
            long frequency = ((Number) value).longValue();
            if (frequency / 60 / 60 > 0) {
                return String.format("%02dh", frequency / 60 / 60);
            } else if (frequency / 60 > 0) {
                return String.format("%02dm", frequency / 60);
            } else {
                return String.format("%02ds", frequency);
            }
        }

        return null;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return formatCommand(command, "**,imei:%s,%s", Command.KEY_UNIQUE_ID, Command.KEY_DATA);
            case Command.TYPE_POSITION_STOP:
                return formatCommand(command, "**,imei:%s,D", Command.KEY_UNIQUE_ID);
            case Command.TYPE_POSITION_SINGLE:
                return formatCommand(command, "**,imei:%s,B", Command.KEY_UNIQUE_ID);
            case Command.TYPE_POSITION_PERIODIC:
                return formatCommand(
                        command, "**,imei:%s,C,%s", this, Command.KEY_UNIQUE_ID, Command.KEY_FREQUENCY);
            case Command.TYPE_ENGINE_STOP:
                return formatCommand(command, "**,imei:%s,J", Command.KEY_UNIQUE_ID);
            case Command.TYPE_ENGINE_RESUME:
                return formatCommand(command, "**,imei:%s,K", Command.KEY_UNIQUE_ID);
            case Command.TYPE_ALARM_ARM:
                return formatCommand(command, "**,imei:%s,L", Command.KEY_UNIQUE_ID);
            case Command.TYPE_ALARM_DISARM:
                return formatCommand(command, "**,imei:%s,M", Command.KEY_UNIQUE_ID);
            case Command.TYPE_REQUEST_PHOTO:
                return formatCommand(command, "**,imei:%s,160", Command.KEY_UNIQUE_ID);
            default:
                return null;
        }
    }

}
