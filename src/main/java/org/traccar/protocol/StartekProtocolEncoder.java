/*
 * Copyright 2021 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.Channel;
import org.traccar.Protocol;
import org.traccar.StringProtocolEncoder;
import org.traccar.helper.Checksum;
import org.traccar.model.Command;

public class StartekProtocolEncoder extends StringProtocolEncoder {

    public StartekProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected String formatCommand(Command command, String format, String... keys) {
        String uniqueId = getUniqueId(command.getDeviceId());
        String payload = super.formatCommand(command, format, keys);
        int length = 1 + uniqueId.length() + 1 + payload.length();
        String sentence = "$$:" + length + "," + uniqueId + "," + payload;
        return sentence + Checksum.sum(sentence) + "\r\n";
    }

    @Override
    protected Object encodeCommand(Channel channel, Command command) {

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> formatCommand(command, "%s", Command.KEY_DATA);
            case Command.TYPE_OUTPUT_CONTROL ->
                    formatCommand(command, "900,%s,%s", Command.KEY_INDEX, Command.KEY_DATA);
            case Command.TYPE_ENGINE_STOP -> formatCommand(command, "900,1,1");
            case Command.TYPE_ENGINE_RESUME -> formatCommand(command, "900,1,0");
            default -> null;
        };
    }

}
