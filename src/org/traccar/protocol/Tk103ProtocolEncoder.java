/*
 * Copyright 2017 Christoph Krey (c@ckrey.de)
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import org.traccar.Context;
import org.traccar.StringProtocolEncoder;
import org.traccar.helper.Log;
import org.traccar.model.Command;

public class Tk103ProtocolEncoder extends StringProtocolEncoder {

    private final boolean forceAlternative;

    public Tk103ProtocolEncoder() {
        this.forceAlternative = false;
    }

    public Tk103ProtocolEncoder(boolean forceAlternative) {
        this.forceAlternative = forceAlternative;
    }

    private String formatAlt(Command command, String format, String... keys) {
        return formatCommand(command, "[begin]sms2," + format + ",[end]", keys);
    }

    @Override
    protected Object encodeCommand(Command command) {

        boolean alternative = forceAlternative || Context.getIdentityManager().lookupAttributeBoolean(
                command.getDeviceId(), "tk103.alternative", false, true);

        initDevicePassword(command, "123456");

        if (alternative) {
            switch (command.getType()) {
                case Command.TYPE_GET_VERSION:
                    return formatAlt(command, "*about*");
                case Command.TYPE_REBOOT_DEVICE:
                    return formatAlt(command, "88888888");
                case Command.TYPE_POSITION_SINGLE:
                    return formatAlt(command, "*getposl*");
                case Command.TYPE_POSITION_PERIODIC:
                    return formatAlt(command, "*routetrack*99*");
                case Command.TYPE_POSITION_STOP:
                    return formatAlt(command, "*routetrackoff*");
                case Command.TYPE_CUSTOM:
                    return formatAlt(command, "{%s}", Command.KEY_DATA);
                case Command.TYPE_GET_DEVICE_STATUS:
                    return formatAlt(command, "*status*");
                case Command.TYPE_IDENTIFICATION:
                    return formatAlt(command, "999999");
                case Command.TYPE_MODE_DEEP_SLEEP:
                    return formatAlt(command, command.getBoolean(Command.KEY_ENABLE) ? "*sleep*2*" : "*sleepoff*");
                case Command.TYPE_MODE_POWER_SAVING:
                    return formatAlt(command, command.getBoolean(Command.KEY_ENABLE) ? "*sleepv*" : "*sleepoff*");
                case Command.TYPE_ALARM_SOS:
                    return formatAlt(command, command.getBoolean(Command.KEY_ENABLE) ? "*soson*" : "*sosoff*");
                case Command.TYPE_SET_CONNECTION:
                    return formatAlt(command, "*setip*%s*{%s}*",
                            command.getString(Command.KEY_SERVER).replace(".", "*"), Command.KEY_PORT);
                case Command.TYPE_SOS_NUMBER:
                    return formatAlt(command, "*master*{%s}*{%s}*", Command.KEY_DEVICE_PASSWORD, Command.KEY_PHONE);
                default:
                    Log.warning(new UnsupportedOperationException(command.getType()));
                    return null;
            }
        } else {
            switch (command.getType()) {
                case Command.TYPE_GET_VERSION:
                    return formatCommand(command, "({%s}AP07)", Command.KEY_UNIQUE_ID);
                case Command.TYPE_REBOOT_DEVICE:
                    return formatCommand(command, "({%s}AT00)", Command.KEY_UNIQUE_ID);
                case Command.TYPE_SET_ODOMETER:
                    return formatCommand(command, "({%s}AX01)", Command.KEY_UNIQUE_ID);
                case Command.TYPE_POSITION_SINGLE:
                    return formatCommand(command, "({%s}AP00)", Command.KEY_UNIQUE_ID);
                case Command.TYPE_POSITION_PERIODIC:
                    return formatCommand(command, "({%s}AR00%s0000)", Command.KEY_UNIQUE_ID,
                            String.format("%04X", command.getInteger(Command.KEY_FREQUENCY)));
                case Command.TYPE_POSITION_STOP:
                    return formatCommand(command, "({%s}AR0000000000)", Command.KEY_UNIQUE_ID);
                case Command.TYPE_ENGINE_STOP:
                    return formatCommand(command, "({%s}AV010)", Command.KEY_UNIQUE_ID);
                case Command.TYPE_ENGINE_RESUME:
                    return formatCommand(command, "({%s}AV011)", Command.KEY_UNIQUE_ID);
                default:
                    Log.warning(new UnsupportedOperationException(command.getType()));
                    return null;
            }
        }
    }

}
