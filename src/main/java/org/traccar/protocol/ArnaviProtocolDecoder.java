/*
 * Copyright 2020 - 2022 Anton Tananaev (anton@traccar.org)
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

import com.google.inject.Injector;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Protocol;

import jakarta.inject.Inject;
import java.net.SocketAddress;

public class ArnaviProtocolDecoder extends BaseProtocolDecoder {

    private final ArnaviTextProtocolDecoder textProtocolDecoder;
    private final ArnaviBinaryProtocolDecoder binaryProtocolDecoder;

    public ArnaviProtocolDecoder(Protocol protocol) {
        super(protocol);
        textProtocolDecoder = new ArnaviTextProtocolDecoder(protocol);
        binaryProtocolDecoder = new ArnaviBinaryProtocolDecoder(protocol);
    }

    @Inject
    public void setInjector(Injector injector) {
        injector.injectMembers(textProtocolDecoder);
        injector.injectMembers(binaryProtocolDecoder);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.getByte(buf.readerIndex()) == '$') {
            return textProtocolDecoder.decode(channel, remoteAddress, msg);
        } else {
            return binaryProtocolDecoder.decode(channel, remoteAddress, msg);
        }
    }

}
