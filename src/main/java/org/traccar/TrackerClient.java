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
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.util.concurrent.TimeUnit;

public abstract class TrackerClient implements TrackerConnector {

    private final boolean secure;
    private final long interval;

    private final Bootstrap bootstrap;

    private final int port;
    private final String address;
    private final String[] devices;

    private final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public boolean isDatagram() {
        return false;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    public TrackerClient(Config config, String protocol) {
        secure = config.getBoolean(Keys.PROTOCOL_SSL.withPrefix(protocol));
        interval = config.getLong(Keys.PROTOCOL_INTERVAL.withPrefix(protocol));
        address = config.getString(Keys.PROTOCOL_ADDRESS.withPrefix(protocol));
        port = config.getInteger(Keys.PROTOCOL_PORT.withPrefix(protocol), secure ? 443 : 80);
        devices = config.getString(Keys.PROTOCOL_DEVICES.withPrefix(protocol)).split("[, ]");

        BasePipelineFactory pipelineFactory = new BasePipelineFactory(this, config, protocol) {
            @Override
            protected void addTransportHandlers(PipelineBuilder pipeline) {
                try {
                    if (isSecure()) {
                        SSLEngine engine = SSLContext.getDefault().createSSLEngine();
                        engine.setUseClientMode(true);
                        pipeline.addLast(new SslHandler(engine));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                try {
                    TrackerClient.this.addProtocolHandlers(pipeline, config);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        bootstrap = new Bootstrap()
                .group(EventLoopGroupFactory.getWorkerGroup())
                .channel(NioSocketChannel.class)
                .handler(pipelineFactory);
    }

    protected abstract void addProtocolHandlers(PipelineBuilder pipeline, Config config) throws Exception;

    public String[] getDevices() {
        return devices;
    }

    @Override
    public ChannelGroup getChannelGroup() {
        return channelGroup;
    }

    @Override
    public void start() throws Exception {
        bootstrap.connect(address, port)
                .syncUninterruptibly().channel().closeFuture().addListener(new GenericFutureListener<>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) {
                        if (interval > 0) {
                            GlobalEventExecutor.INSTANCE.schedule(() -> {
                                bootstrap.connect(address, port)
                                        .syncUninterruptibly().channel().closeFuture().addListener(this);
                            }, interval, TimeUnit.SECONDS);
                        }
                    }
                });
    }

    @Override
    public void stop() {
        channelGroup.close().awaitUninterruptibly();
    }

}
