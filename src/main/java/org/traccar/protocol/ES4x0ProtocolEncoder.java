/*
 * Copyright 2024 - 2025 Anton Tananaev (anton@traccar.org)
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

import org.traccar.Protocol;
import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;
import org.traccar.model.Position;

public class ES4x0ProtocolEncoder extends StringProtocolEncoder {

    private static final String PASSWORD = "000000";

    public ES4x0ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return formatCommand(command, "%s,%s,%s#", Command.KEY_DATA, "", "");
            case Command.TYPE_POSITION_SINGLE:
                return "report," + PASSWORD + ",20#";
            case Command.TYPE_POSITION_PERIODIC:
                return "interval," + PASSWORD + "," + command.getInteger(Command.KEY_FREQUENCY) + "#";
            case Command.TYPE_ENGINE_STOP:
                return "sidoff,0#";
            case Command.TYPE_ENGINE_RESUME:
                return "sidon,0#";
            case Command.TYPE_REBOOT_DEVICE:
                return "reboot," + PASSWORD + "#";
            case Command.TYPE_GET_VERSION:
                return "version," + PASSWORD + "?#";
            case Command.TYPE_GET_DEVICE_STATUS:
                return "getid," + PASSWORD + "?#";
            case Command.TYPE_SET_ODOMETER:
                return "mileage," + PASSWORD + "," + command.getInteger(Position.KEY_ODOMETER) + "#";
            case Command.TYPE_OUTPUT_CONTROL:
                int outputIndex = command.getInteger("index");
                boolean enable = command.getBoolean(Command.KEY_ENABLE);
                return (enable ? "sidon," : "sidoff,") + outputIndex + "#";
            case "interval":
                return "interval," + PASSWORD + "," + command.getString(Command.KEY_DATA) + "#";
            case "nomove":
                return "nomove," + PASSWORD + "," + command.getString(Command.KEY_DATA) + "#";
            case "speedalarm":
                return "speedalarm," + PASSWORD + "," + command.getString(Command.KEY_DATA) + "#";
            case "apn":
                return "apn," + PASSWORD + "," + command.getString(Command.KEY_DATA) + "#";
            case "heartbeat":
                return "heartbeat," + PASSWORD + "," + command.getString(Command.KEY_DATA) + "#";
            case "sleepmode":
                return "sleepmode," + PASSWORD + "," + command.getString(Command.KEY_DATA) + "#";
            case "data":
                return "data," + PASSWORD + "," + command.getString(Command.KEY_DATA) + "#";
            default:
                return null;
        }
    }

}
