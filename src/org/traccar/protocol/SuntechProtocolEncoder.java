/*
 * Copyright 2015 - 2016 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Log;
import org.traccar.model.Command;

public class SuntechProtocolEncoder extends StringProtocolEncoder {

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_REBOOT_DEVICE:
                return formatCommand(command, "SA200CMD;{%s};02;Reboot\r", Command.KEY_UNIQUE_ID);
            case Command.TYPE_POSITION_SINGLE:
                return formatCommand(command, "SA200GTR;{%s};02;\r", Command.KEY_UNIQUE_ID);
            case Command.TYPE_OUTPUT_CONTROL:
                if (command.getAttributes().containsKey(Command.KEY_DATA)) {
                    if (command.getAttributes().get(Command.KEY_DATA).equals("1")) {
                        return formatCommand(command, "SA200CMD;{%s};02;Enable{%s}\r",
                                Command.KEY_UNIQUE_ID, Command.KEY_INDEX);
                    } else {
                        return formatCommand(command, "SA200CMD;{%s};02;Disable{%s}\r",
                                Command.KEY_UNIQUE_ID, Command.KEY_INDEX);
                    }
                }
            case Command.TYPE_ENGINE_STOP:
                return formatCommand(command, "SA200CMD;{%s};02;Enable1\r", Command.KEY_UNIQUE_ID);
            case Command.TYPE_ENGINE_RESUME:
                return formatCommand(command, "SA200CMD;{%s};02;Disable1\r", Command.KEY_UNIQUE_ID);
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
