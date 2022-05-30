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
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;

import java.net.SocketAddress;

public class R12wProtocolDecoder extends BaseProtocolDecoder {

    public R12wProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, String type, String id, String data) {
        if (channel != null) {
            String sentence = String.format("$HX,%s,%s,%s,#", type, id, data);
            sentence += String.format(",%02x,\r\n", Checksum.xor(sentence));
            channel.writeAndFlush(new NetworkMessage(sentence, channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        String[] values = sentence.split(",");
        String type = values[1];
        String id = values[2];

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        if (type.equals("0001")) {
            sendResponse(channel, "1001", id, values[3] + ",OK");
        }

        return null;
    }

}
