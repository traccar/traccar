/*
 * Copyright 2016 - 2019 Anton Tananaev (anton@traccar.org)
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

import org.traccar.model.Command;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;

public class TotemProtocolEncoder extends TotemProtocolSmsEncoder {

    public TotemProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private String encodeCommand(String commandString) {
        String builtCommand = String.format("$$%04dCF%s", 10 + commandString.getBytes().length, commandString);
        return String.format("%s%02X", builtCommand, Checksum.xor(builtCommand));
    }

    @Override
    protected String encodeCommand(Command command) {

        initDevicePassword(command, "000000");

        return encodeCommand(super.getCommandString(command));
    }

}
