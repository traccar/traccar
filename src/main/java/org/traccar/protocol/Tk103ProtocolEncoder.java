/*
 * Copyright 2017 Christoph Krey (c@ckrey.de)
 * Copyright 2017 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.config.Keys;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.Command;

public class Tk103ProtocolEncoder extends StringProtocolEncoder {

    private final boolean forceAlternative;

    public Tk103ProtocolEncoder(Protocol protocol) {
        super(protocol);
        this.forceAlternative = false;
    }

    public Tk103ProtocolEncoder(Protocol protocol, boolean forceAlternative) {
        super(protocol);
        this.forceAlternative = forceAlternative;
    }

    private String formatAlt(Command command, String format, String... keys) {
        return formatCommand(command, "[begin]sms2," + format + ",[end]", keys);
    }

    @Override
    protected Object encodeCommand(Command command) {

        boolean alternative = AttributeUtil.lookup(
                getCacheManager(), Keys.PROTOCOL_ALTERNATIVE.withPrefix(getProtocolName()), command.getDeviceId());

        initDevicePassword(command, "123456");

        if (alternative || forceAlternative) {
            switch (command.getType()) {
                case Command.TYPE_CUSTOM:
                    return formatAlt(command, "%s", Command.KEY_DATA);
                case Command.TYPE_GET_VERSION:
                    return formatAlt(command, "*about*");
                case Command.TYPE_POWER_OFF:
                    return formatAlt(command, "*turnoff*");
                case Command.TYPE_REBOOT_DEVICE:
                    return formatAlt(command, "88888888");
                case Command.TYPE_POSITION_SINGLE:
                    return formatAlt(command, "*getposl*");
                case Command.TYPE_POSITION_PERIODIC:
                    return formatAlt(command, "*routetrack*99*");
                case Command.TYPE_POSITION_STOP:
                    return formatAlt(command, "*routetrackoff*");
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
                    String server = command.getString(Command.KEY_SERVER).replace(".", "*");
                    return formatAlt(command, "*setip*" + server + "*%s*", Command.KEY_PORT);
                case Command.TYPE_SOS_NUMBER:
                    return formatAlt(command, "*master*%s*%s*", Command.KEY_DEVICE_PASSWORD, Command.KEY_PHONE);
                default:
                    return null;
            }
        } else {
            switch (command.getType()) {
                case Command.TYPE_CUSTOM:
                    return formatCommand(command, "(%s%s)", Command.KEY_UNIQUE_ID, Command.KEY_DATA);
                case Command.TYPE_GET_VERSION:
                    return formatCommand(command, "(%sAP07)", Command.KEY_UNIQUE_ID);
                case Command.TYPE_REBOOT_DEVICE:
                    return formatCommand(command, "(%sAT00)", Command.KEY_UNIQUE_ID);
                case Command.TYPE_SET_ODOMETER:
                    return formatCommand(command, "(%sAX01)", Command.KEY_UNIQUE_ID);
                case Command.TYPE_POSITION_SINGLE:
                    return formatCommand(command, "(%sAP00)", Command.KEY_UNIQUE_ID);
                case Command.TYPE_POSITION_PERIODIC:
                    String frequency = String.format("%04X", command.getInteger(Command.KEY_FREQUENCY));
                    return formatCommand(command, "(%sAR00" + frequency + "0000)", Command.KEY_UNIQUE_ID);
                case Command.TYPE_POSITION_STOP:
                    return formatCommand(command, "(%sAR0000000000)", Command.KEY_UNIQUE_ID);
                case Command.TYPE_ENGINE_STOP:
                    return formatCommand(command, "(%sAV010)", Command.KEY_UNIQUE_ID);
                case Command.TYPE_ENGINE_RESUME:
                    return formatCommand(command, "(%sAV011)", Command.KEY_UNIQUE_ID);
                case Command.TYPE_OUTPUT_CONTROL:
                    return formatCommand(command, "(%sAV00%s)", Command.KEY_UNIQUE_ID, Command.KEY_DATA);
                default:
                    return null;
            }
        }
    }

}
