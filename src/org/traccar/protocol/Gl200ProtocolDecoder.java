/*
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;

import java.net.SocketAddress;

public class Gl200ProtocolDecoder extends BaseProtocolDecoder {

    private final Gl200TextProtocolDecoder textProtocolDecoder;
    private final Gl200BinaryProtocolDecoder binaryProtocolDecoder;

    public Gl200ProtocolDecoder(Gl200Protocol protocol) {
        super(protocol);
        textProtocolDecoder = new Gl200TextProtocolDecoder(protocol);
        binaryProtocolDecoder = new Gl200BinaryProtocolDecoder(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        if (Gl200FrameDecoder.isBinary(buf)) {
            return binaryProtocolDecoder.decode(channel, remoteAddress, msg);
        } else {
            return textProtocolDecoder.decode(channel, remoteAddress, msg);
        }
    }

}
