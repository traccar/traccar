/*
 * Copyright 2015 Irving Gonzalez
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;
import org.traccar.Protocol;

public class TotemProtocolSmsEncoder extends StringProtocolEncoder {

    public TotemProtocolSmsEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Command command) {

        initDevicePassword(command, "000000");

        return String.format("*%s#", TotemProtocolEncoder.formatContent(command));
    }

}
