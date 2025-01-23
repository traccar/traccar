/*
 * Copyright 2017 - 2019 Anton Tananaev (anton@digitalegiz.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@digitalegiz.org)
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
package org.digitalegiz.protocol;

import org.digitalegiz.StringProtocolEncoder;
import org.digitalegiz.model.Command;
import org.digitalegiz.Protocol;

public class GranitProtocolSmsEncoder extends StringProtocolEncoder {

    public GranitProtocolSmsEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected String encodeCommand(Command command) {
        return switch (command.getType()) {
            case Command.TYPE_REBOOT_DEVICE -> "BB+RESET";
            case Command.TYPE_POSITION_PERIODIC -> formatCommand(command, "BB+BBMD=%s", Command.KEY_FREQUENCY);
            default -> null;
        };
    }

}
