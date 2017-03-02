/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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

public class GranitProtocolSmsEncoder extends StringProtocolEncoder {

    @Override
    protected String encodeCommand(Command command) {
        switch (command.getType()) {
        case Command.TYPE_REBOOT_DEVICE:
            return "BB+RESET";
        case Command.TYPE_POSITION_PERIODIC:
            return formatCommand(command, "BB+BBMD={%s}", Command.KEY_FREQUENCY);
        default:
            Log.warning(new UnsupportedOperationException(command.getType()));
            return null;
        }
    }

}
