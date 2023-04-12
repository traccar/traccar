/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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
package org.traccar.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import org.traccar.forward.NetworkForwarder;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class NetworkForwarderHandler extends ChannelInboundHandlerAdapter {

    private final int port;

    private NetworkForwarder networkForwarder;

    public NetworkForwarderHandler(int port) {
        this.port = port;
    }

    @Inject
    public void setNetworkForwarder(NetworkForwarder networkForwarder) {
        this.networkForwarder = networkForwarder;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        boolean datagram = ctx.channel() instanceof DatagramChannel;
        SocketAddress remoteAddress;
        ByteBuf buffer;
        if (datagram) {
            DatagramPacket message = (DatagramPacket) msg;
            remoteAddress = message.recipient();
            buffer = message.content();
        } else {
            remoteAddress = ctx.channel().remoteAddress();
            buffer = (ByteBuf) msg;
        }

        byte[] data = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), data);
        networkForwarder.forward((InetSocketAddress) remoteAddress, port, datagram, data);
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!(ctx.channel() instanceof DatagramChannel)) {
            networkForwarder.disconnect((InetSocketAddress) ctx.channel().remoteAddress());
        }
        super.channelInactive(ctx);
    }

}
