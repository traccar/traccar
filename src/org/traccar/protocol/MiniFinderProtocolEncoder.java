/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

public class MiniFinderProtocolEncoder extends StringProtocolEncoder {

    private int getEnabledFlag(Command command) {
        return (Boolean) command.getAttributes().get(Command.KEY_ENABLE) ? 1 : 0;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_SET_TIMEZONE:
                return String.format("123456L%+03d",
                        ((Number) command.getAttributes().get(Command.KEY_TIMEZONE)).longValue() / 3600);
            case Command.TYPE_VOICE_MONITORING:
                return String.format("123456L%d", getEnabledFlag(command));
            case Command.TYPE_ALARM_SPEED:
                return formatCommand(command, "123456J1{%s}", Command.KEY_DATA);
            case Command.TYPE_ALARM_GEOFENCE:
                return formatCommand(command, "123456R1{%s}", Command.KEY_RADIUS);
            case Command.TYPE_SET_AGPS:
                return String.format("123456AGPS%d", getEnabledFlag(command));
            case Command.TYPE_ALARM_FALL:
                return String.format("123456F%d", getEnabledFlag(command));
            case Command.TYPE_MODE_POWER_SAVING:
                return String.format("123456SP%d", getEnabledFlag(command));
            case Command.TYPE_MODE_DEEP_SLEEP:
                return String.format("123456DS%d", getEnabledFlag(command));
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                return null;
        }
    }

}
