/*
 * Copyright 2018 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocolEncoder;
import org.traccar.model.Command;
import org.traccar.Protocol;

public class Xrb28ProtocolEncoder extends BaseProtocolEncoder {

    public Xrb28ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private String formatCommand(Command command, String content) {
        return String.format("\u00ff\u00ff*SCOS,OM,%s,%s#\n", getUniqueId(command.getDeviceId()), content);
    }

    @Override
    protected Object encodeCommand(Channel channel, Command command) {

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> formatCommand(command, command.getString(Command.KEY_DATA));
            case Command.TYPE_POSITION_SINGLE -> formatCommand(command, "D0");
            case Command.TYPE_POSITION_PERIODIC ->
                    formatCommand(command, "D1," + command.getInteger(Command.KEY_FREQUENCY));
            case Command.TYPE_ENGINE_STOP, Command.TYPE_ALARM_DISARM -> {
                if (channel != null) {
                    Xrb28ProtocolDecoder decoder = channel.pipeline().get(Xrb28ProtocolDecoder.class);
                    if (decoder != null) {
                        decoder.setPendingCommand(command.getType());
                    }
                }
                yield formatCommand(command, "R0,0,20,1234," + System.currentTimeMillis() / 1000);
            }
            default -> null;
        };
    }

}
