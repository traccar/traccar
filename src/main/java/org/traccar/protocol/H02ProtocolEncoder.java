/*
 * Copyright 2016 Gabor Somogyi (gabor.g.somogyi@gmail.com)
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

import org.traccar.Protocol;
import org.traccar.StringProtocolEncoder;
import org.traccar.config.Keys;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.Command;

import java.util.Date;

public class H02ProtocolEncoder extends StringProtocolEncoder {

    private static final String MARKER = "HQ";

    public H02ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private Object formatCommand(Date time, String uniqueId, String type, String... params) {

        StringBuilder result = new StringBuilder(
                String.format("*%s,%s,%s,%4$tH%4$tM%4$tS", MARKER, uniqueId, type, time));

        for (String param : params) {
            result.append(",").append(param);
        }

        result.append("#");

        return result.toString();
    }

    protected Object encodeCommand(Command command, Date time) {
        String uniqueId = getUniqueId(command.getDeviceId());

        switch (command.getType()) {
            case Command.TYPE_ALARM_ARM:
                return formatCommand(time, uniqueId, "SCF", "0", "0");
            case Command.TYPE_ALARM_DISARM:
                return formatCommand(time, uniqueId, "SCF", "1", "1");
            case Command.TYPE_ENGINE_STOP:
                return formatCommand(time, uniqueId, "S20", "1", "1");
            case Command.TYPE_ENGINE_RESUME:
                return formatCommand(time, uniqueId, "S20", "1", "0");
            case Command.TYPE_POSITION_PERIODIC:
                String frequency = command.getAttributes().get(Command.KEY_FREQUENCY).toString();
                if (AttributeUtil.lookup(
                        getCacheManager(), Keys.PROTOCOL_ALTERNATIVE.withPrefix(getProtocolName()),
                        command.getDeviceId())) {
                    return formatCommand(time, uniqueId, "D1", frequency);
                } else {
                    return formatCommand(time, uniqueId, "S71", "22", frequency);
                }
            default:
                return null;
        }
    }

    @Override
    protected Object encodeCommand(Command command) {
        return encodeCommand(command, new Date());
    }

}
