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

    @Override
    protected Object encodeCommand(Command command) {

        boolean alternative = Context.getIdentityManager().lookupAttributeBoolean(
                command.getDeviceId(), "tk103.alternative", false, true);

        initDevicePassword(command, "123456");

        switch (command.getType()) {
            case Command.TYPE_GET_VERSION:
                return alternative ? formatCommand(command, "[begin]sms2,*about*,[end]")
                        : formatCommand(command, "({%s}AP07)", Command.KEY_UNIQUE_ID);
            case Command.TYPE_REBOOT_DEVICE:
                return alternative ? formatCommand(command, "[begin]sms2,88888888,[end]")
                        : formatCommand(command, "({%s}AT00)", Command.KEY_UNIQUE_ID);
            case Command.TYPE_SET_ODOMETER:
                return formatCommand(command, "({%s}AX01)", Command.KEY_UNIQUE_ID);
            case Command.TYPE_POSITION_SINGLE:
                return alternative ? formatCommand(command, "[begin]sms2,*getposl*,[end]")
                        : formatCommand(command, "({%s}AP00)", Command.KEY_UNIQUE_ID);
            case Command.TYPE_POSITION_PERIODIC:
                return alternative ? formatCommand(command, "[begin]sms2,*routetrack*99*,[end]")
                        : formatCommand(command, "({%s}AR00%s0000)", Command.KEY_UNIQUE_ID,
                        String.format("%04X", command.getInteger(Command.KEY_FREQUENCY)));
            case Command.TYPE_POSITION_STOP:
                return alternative ? formatCommand(command, "[begin]sms2,*routetrackoff*,[end]")
                        : formatCommand(command, "({%s}AR0000000000)", Command.KEY_UNIQUE_ID);
            case Command.TYPE_ENGINE_STOP:
                return formatCommand(command, "({%s}AV011)", Command.KEY_UNIQUE_ID);
            case Command.TYPE_ENGINE_RESUME:
                return formatCommand(command, "({%s}AV010)", Command.KEY_UNIQUE_ID);
            case Command.TYPE_CUSTOM:
                return formatCommand(command, "[begin]sms2,{%s},[end]", Command.KEY_DATA);
            case Command.TYPE_GET_DEVICE_STATUS:
                return formatCommand(command, "[begin]sms2,*status*,[end]");
            case Command.TYPE_IDENTIFICATION:
                return formatCommand(command, "[begin]sms2,999999,[end]");
            case Command.TYPE_MODE_DEEP_SLEEP:
                return formatCommand(command, command.getBoolean(Command.KEY_ENABLE)
                        ? "[begin]sms2,*sleep*2*,[end]" : "[begin]sms2,*sleepoff*,[end]");
            case Command.TYPE_MODE_POWER_SAVING:
                return formatCommand(command, command.getBoolean(Command.KEY_ENABLE)
                        ? "[begin]sms2,*sleepv*,[end]" : "[begin]sms2,*sleepoff*,[end]");
            case Command.TYPE_ALARM_SOS:
                return formatCommand(command, command.getBoolean(Command.KEY_ENABLE)
                        ? "[begin]sms2,*soson*,[end]" : "[begin]sms2,*sosoff*,[end]");
            case Command.TYPE_SET_CONNECTION:
                return formatCommand(command, "[begin]sms2,*setip*%s*{%s}*,[end]",
                        command.getString(Command.KEY_SERVER).replace(".", "*"), Command.KEY_PORT);
            case Command.TYPE_SOS_NUMBER:
                return formatCommand(command, "[begin]sms2,*master*{%s}*{%s}*,[end]",
                        Command.KEY_DEVICE_PASSWORD, Command.KEY_PHONE);
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
