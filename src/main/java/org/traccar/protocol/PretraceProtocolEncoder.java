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

import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.model.Command;

public class PretraceProtocolEncoder extends BaseProtocolEncoder {

    public PretraceProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private String formatCommand(String uniqueId, String data) {
        String content = uniqueId + data;
        return String.format("(%s^%02X)", content, Checksum.xor(content));
    }

    @Override
    protected Object encodeCommand(Command command) {

        String uniqueId = getUniqueId(command.getDeviceId());

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return formatCommand(uniqueId, command.getString(Command.KEY_DATA));
            case Command.TYPE_POSITION_PERIODIC:
                return formatCommand(
                        uniqueId, String.format("D221%1$d,%1$d,,", command.getInteger(Command.KEY_FREQUENCY)));
            default:
                return null;
        }
    }

}
