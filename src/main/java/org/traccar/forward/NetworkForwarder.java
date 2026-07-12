/*
 * Copyright 2023 - 2026 Anton Tananaev (anton@traccar.org)
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
package org.traccar.forward;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.EventLoopGroupFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class NetworkForwarder {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkForwarder.class);

    private final String destination;

    private final Bootstrap tcpBootstrap;
    private final Channel udpChannel;
    private final Map<InetSocketAddress, ChannelFuture> tcpConnects = new ConcurrentHashMap<>();

    @Inject
    public NetworkForwarder(
            Config config, EventLoopGroupFactory eventLoopGroupFactory) throws InterruptedException {
        destination = config.getString(Keys.SERVER_FORWARD);
        int connectTimeout = config.getInteger(Keys.SERVER_FORWARD_CONNECT_TIMEOUT);
        int writeTimeout = config.getInteger(Keys.SERVER_FORWARD_WRITE_TIMEOUT);

        tcpBootstrap = new Bootstrap()
                .group(eventLoopGroupFactory.getWorkerGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline().addLast(
                                new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS));
                        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
                                context.close();
                            }
                        });
                    }
                });

        udpChannel = new Bootstrap()
                .group(eventLoopGroupFactory.getWorkerGroup())
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel channel) {
                    }
                })
                .bind(0).sync().channel();
    }

    public void forward(InetSocketAddress source, int port, boolean datagram, byte[] data) {
        if (datagram) {
            udpChannel.writeAndFlush(new DatagramPacket(
                    Unpooled.wrappedBuffer(data), new InetSocketAddress(destination, port)))
                    .addListener(this::logFailure);
            return;
        }
        ChannelFuture connectFuture = tcpConnects.get(source);
        if (connectFuture == null) {
            connectFuture = tcpBootstrap.connect(destination, port);
            tcpConnects.put(source, connectFuture);
            ChannelFuture registered = connectFuture;
            connectFuture.channel().closeFuture().addListener(
                    future -> tcpConnects.remove(source, registered));
        }
        ChannelFuture pending = connectFuture;
        connectFuture.addListener(future -> {
            if (future.isSuccess()) {
                pending.channel().writeAndFlush(Unpooled.wrappedBuffer(data)).addListener(this::logFailure);
            } else {
                logFailure(future);
            }
        });
    }

    public void disconnect(InetSocketAddress source) {
        ChannelFuture connectFuture = tcpConnects.remove(source);
        if (connectFuture != null) {
            connectFuture.channel().close();
        }
    }

    private void logFailure(Future<?> future) {
        if (!future.isSuccess()) {
            LOGGER.warn("Network forwarding error", future.cause());
        }
    }

}
