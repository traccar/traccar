/*
 * Copyright 2017 - 2020 Anton Tananaev (anton@traccar.org)
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
package org.traccar;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public abstract class BaseHttpProtocolDecoder extends BaseProtocolDecoder {

    public BaseHttpProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public void sendResponse(Channel channel, HttpResponseStatus status) {
        sendResponse(channel, status, null);
    }

    public void sendResponse(Channel channel, HttpResponseStatus status, ByteBuf buf) {
        if (channel != null) {
            if (buf == null) {
                buf = Unpooled.buffer(0);
            }
            HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buf);
            response.headers().add(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

}
