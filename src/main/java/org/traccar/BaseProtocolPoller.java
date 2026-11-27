/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public abstract class BaseProtocolPoller extends ChannelDuplexHandler {

    private final long interval;
    private Future<?> timeout;

    public BaseProtocolPoller(long interval) {
        this.interval = interval;
    }

    protected abstract void sendRequest(Channel channel, SocketAddress remoteAddress);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        if (interval > 0) {
            timeout = ctx.executor().scheduleAtFixedRate(
                    () -> sendRequest(ctx.channel(), ctx.channel().remoteAddress()), 0, interval, TimeUnit.SECONDS);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (timeout != null) {
            timeout.cancel(false);
            timeout = null;
        }
    }

}
