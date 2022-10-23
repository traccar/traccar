/*
 * Copyright 2012 - 2022 Anton Tananaev (anton@traccar.org)
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

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;

public abstract class TrackerServer implements TrackerConnector {

    private final boolean datagram;
    private final boolean secure;

    @SuppressWarnings("rawtypes")
    private final AbstractBootstrap bootstrap;

    private final int port;
    private final String address;

    private final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public boolean isDatagram() {
        return datagram;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    public TrackerServer(Config config, String protocol, boolean datagram) {
        secure = config.getBoolean(Keys.PROTOCOL_SSL.withPrefix(protocol));
        address = config.getString(Keys.PROTOCOL_ADDRESS.withPrefix(protocol));
        port = config.getInteger(Keys.PROTOCOL_PORT.withPrefix(protocol));

        BasePipelineFactory pipelineFactory = new BasePipelineFactory(this, config, protocol) {
            @Override
            protected void addTransportHandlers(PipelineBuilder pipeline) {
                try {
                    if (isSecure()) {
                        SSLEngine engine = SSLContext.getDefault().createSSLEngine();
                        pipeline.addLast(new SslHandler(engine));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                TrackerServer.this.addProtocolHandlers(pipeline, config);
            }
        };

        this.datagram = datagram;
        if (datagram) {
            bootstrap = new Bootstrap()
                    .group(EventLoopGroupFactory.getWorkerGroup())
                    .channel(NioDatagramChannel.class)
                    .handler(pipelineFactory);
        } else {
            bootstrap = new ServerBootstrap()
                    .group(EventLoopGroupFactory.getBossGroup(), EventLoopGroupFactory.getWorkerGroup())
                    .channel(NioServerSocketChannel.class)
                    .childHandler(pipelineFactory);
        }
    }

    protected abstract void addProtocolHandlers(PipelineBuilder pipeline, Config config);

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public ChannelGroup getChannelGroup() {
        return channelGroup;
    }

    @Override
    public void start() throws Exception {
        InetSocketAddress endpoint;
        if (address == null) {
            endpoint = new InetSocketAddress(port);
        } else {
            endpoint = new InetSocketAddress(address, port);
        }

        Channel channel = bootstrap.bind(endpoint).syncUninterruptibly().channel();
        if (channel != null) {
            getChannelGroup().add(channel);
        }
    }

    @Override
    public void stop() {
        channelGroup.close().awaitUninterruptibly();
    }

}
