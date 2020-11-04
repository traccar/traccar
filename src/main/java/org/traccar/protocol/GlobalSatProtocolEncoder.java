/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Checksum;
import org.traccar.model.Command;

public class GlobalSatProtocolEncoder extends StringProtocolEncoder {

    public GlobalSatProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Command command) {

        String formattedCommand = null;

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                formattedCommand = formatCommand(
                        command, "GSC,%s,%s", Command.KEY_UNIQUE_ID, Command.KEY_DATA);
                break;
            case Command.TYPE_ALARM_DISMISS:
                formattedCommand = formatCommand(
                        command, "GSC,%s,Na", Command.KEY_UNIQUE_ID);
                break;
            case Command.TYPE_OUTPUT_CONTROL:
                formattedCommand = formatCommand(
                        command, "GSC,%s,Lo(%s,%s)", Command.KEY_UNIQUE_ID, Command.KEY_INDEX, Command.KEY_DATA);
                break;
            default:
                break;
        }

        if (formattedCommand != null) {
            return formattedCommand + Checksum.nmea(formattedCommand) + '!';
        } else {
            return null;
        }
    }

}
