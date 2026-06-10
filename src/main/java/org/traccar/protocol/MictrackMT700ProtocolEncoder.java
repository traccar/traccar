/*
 * Copyright 2026 Drew Taylor (Drew.Taylor@fognetx.com)
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

public class MictrackMT700ProtocolEncoder extends StringProtocolEncoder {

    public MictrackMT700ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Command command) {
        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> formatCommand(command, "%s", Command.KEY_DATA);
            case Command.TYPE_REBOOT_DEVICE -> "REBOOT";
            case Command.TYPE_POSITION_PERIODIC -> formatCommand(command, "MODE,1,%s", Command.KEY_FREQUENCY);
            case Command.TYPE_MODE_DEEP_SLEEP -> {
                long hours = Math.max(1, Math.min(24,
                        ((Number) command.getAttributes().get(Command.KEY_FREQUENCY)).longValue() / 3600));
                yield String.format("MODE,3,%d", hours);
            }
            case Command.TYPE_SET_CONNECTION -> formatCommand(command, "804,%s,%s", Command.KEY_SERVER, Command.KEY_PORT);
            case Command.TYPE_GET_DEVICE_STATUS -> "RCONF,1";
            default -> null;
        };
    }

}
