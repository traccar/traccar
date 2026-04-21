/*
 * Copyright 2025 Joaquim Cardeira (joaquim@cardeira.org)
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
import org.traccar.helper.DateUtil;
import org.traccar.model.Command;

import java.util.Date;

public class MobilogixProtocolEncoder extends StringProtocolEncoder {

    public MobilogixProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private Object encodeCommand(Date time, String param) {
        return String.format("[%s,%s]", DateUtil.formatDate(time, false), param);
    }

    @Override
    protected Object encodeCommand(Command command) {
        return encodeCommand(command, new Date());
    }

    protected Object encodeCommand(Command command, Date time) {
        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> encodeCommand(time, command.getString(Command.KEY_DATA));
            case Command.TYPE_ENGINE_RESUME -> encodeCommand(time, "S6,RELAY=0");
            case Command.TYPE_ENGINE_STOP -> encodeCommand(time, "S6,RELAY=1");
            case Command.TYPE_POSITION_SINGLE -> encodeCommand(time, "S4,1,1");
            case Command.TYPE_REBOOT_DEVICE -> encodeCommand(time, "S7");
            default -> null;
        };
    }
}
