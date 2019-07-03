/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Protocol;
import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;
import org.traccar.helper.Checksum;

public class LaipacProtocolEncoder extends StringProtocolEncoder {

    private final Protocol protocol;
    private final String defaultDevicePassword;

    public LaipacProtocolEncoder(Protocol protocol) {
        this.protocol = protocol;
        defaultDevicePassword = Context.getConfig().getString(
            getProtocolName() + ".defaultPassword", "00000000");
    }

    protected String getProtocolName() {
        return protocol.getName();
    }

    @Override
    protected Object encodeCommand(Command command) {

        initDevicePassword(command, defaultDevicePassword);

        String cmd = "";

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                cmd = formatCommand(command, "${%s}",
                    Command.KEY_DATA);
                break;
            case Command.TYPE_POSITION_SINGLE:
                cmd = formatCommand(command, "$AVREQ,{%s},1",
                    Command.KEY_DEVICE_PASSWORD);
                break;
            case Command.TYPE_REBOOT_DEVICE:
                cmd = formatCommand(command, "$AVRESET,{%s},{%s}",
                    Command.KEY_UNIQUE_ID, Command.KEY_DEVICE_PASSWORD);
                break;
            default:
                break;
        }

        if (cmd.length() > 0) {
            cmd += Checksum.nmea(cmd) + "\r\n";
            return cmd;
        }

        return null;
    }

}
