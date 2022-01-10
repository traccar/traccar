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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.traccar.config.Keys;

import java.util.List;

public abstract class TrackerClient implements TrackerConnector {

    private final Bootstrap bootstrap;

    private final int port;
    private final String address;
    private final String[] devices;

    private final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public boolean isDatagram() {
        return false;
    }

    public TrackerClient(String protocol) {

        address = Context.getConfig().getString(Keys.PROTOCOL_ADDRESS.withPrefix(protocol));
        port = Context.getConfig().getInteger(Keys.PROTOCOL_PORT.withPrefix(protocol));
        devices = Context.getConfig().getString(Keys.PROTOCOL_DEVICES.withPrefix(protocol)).split("[, ]");

        BasePipelineFactory pipelineFactory = new BasePipelineFactory(this, protocol) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                TrackerClient.this.addProtocolHandlers(pipeline);
            }
        };

        bootstrap = new Bootstrap()
                .group(EventLoopGroupFactory.getWorkerGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        pipelineFactory.initChannel(channel);
                    }
                });
    }

    protected abstract void addProtocolHandlers(PipelineBuilder pipeline);

    @Override
    public ChannelGroup getChannelGroup() {
        return channelGroup;
    }

    @Override
    public void start() throws Exception {
        bootstrap.connect(address, port).sync();
    }

    @Override
    public void stop() {
        channelGroup.close().awaitUninterruptibly();
    }

}
