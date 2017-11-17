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

import org.traccar.StringProtocolEncoder;
import org.traccar.helper.Log;
import org.traccar.model.Command;

public class Tk103ProtocolEncoder extends StringProtocolEncoder {

    protected String t580WFormatCommand(Command command, String format, String... keys) {
        String content = super.formatCommand(command, format, keys);
        return String.format("[begin]sms2,%s,[end]", content);
    }

    @Override
    protected Object encodeCommand(Command command) {

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
                return formatCommand(command, "({%s}AV011)", Command.KEY_UNIQUE_ID);
            case Command.TYPE_ENGINE_RESUME:
                return formatCommand(command, "({%s}AV010)", Command.KEY_UNIQUE_ID);
            //T580W commands:
            case "T580W_positionSingle":
                return t580WFormatCommand(command, "*getposl*");
            case "T580W_positionRealtime":
                return t580WFormatCommand(command, "*routetrack*99*");
            case "T580W_positionRealtimeStop":
                return t580WFormatCommand(command, "*routetrackoff*");
            case "T580W_modeDeepSleepInterval1Hour":
                return t580WFormatCommand(command, "*sleep*1*");
            case "T580W_modeDeepSleepInterval2Hour":
                return t580WFormatCommand(command, "*sleep*2*");
            case "T580W_modeDeepSleepInterval3Hour":
                return t580WFormatCommand(command, "*sleep*3*");
            case "T580W_modeDeepSleepInterval4Hour":
                return t580WFormatCommand(command, "*sleep*4*");
            case "T580W_modeDeepSleepMotion":
                return t580WFormatCommand(command, "*sleepv*");
            case "T580W_modeDeepSleepOff":
                return t580WFormatCommand(command, "*sleepoff*");
            case "T580W_alarmSosOn":
                return t580WFormatCommand(command, "*soson*");
            case "T580W_alarmSosOff":
                return t580WFormatCommand(command, "*sosoff*");
            case "T580W_multiControllerOn":
                return t580WFormatCommand(command, "*multiquery*");
            case "T580W_multiControllerOff":
                return t580WFormatCommand(command, "*multiqueryoff*");
            case "T580W_rebootDevice":
                return t580WFormatCommand(command, "88888888");
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
