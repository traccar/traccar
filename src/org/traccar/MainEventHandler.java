/*
 * Copyright 2012 - 2018 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.timeout.IdleStateEvent;
import org.traccar.helper.Log;
import org.traccar.model.Position;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainEventHandler extends ChannelInboundHandlerAdapter {

    private final Set<String> connectionlessProtocols = new HashSet<>();

    public MainEventHandler() {
        String connectionlessProtocolList = Context.getConfig().getString("status.ignoreOffline");
        if (connectionlessProtocolList != null) {
            connectionlessProtocols.addAll(Arrays.asList(connectionlessProtocolList.split(",")));
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Position) {

            Position position = (Position) msg;
            try {
                Context.getDeviceManager().updateLatestPosition(position);
            } catch (SQLException error) {
                Log.warning(error);
            }

            String uniqueId = Context.getIdentityManager().getById(position.getDeviceId()).getUniqueId();

            // Log position
            StringBuilder s = new StringBuilder();
            s.append(formatChannel(ctx.channel())).append(" ");
            s.append("id: ").append(uniqueId);
            s.append(", time: ").append(
                    new SimpleDateFormat(Log.DATE_FORMAT).format(position.getFixTime()));
            s.append(", lat: ").append(String.format("%.5f", position.getLatitude()));
            s.append(", lon: ").append(String.format("%.5f", position.getLongitude()));
            if (position.getSpeed() > 0) {
                s.append(", speed: ").append(String.format("%.1f", position.getSpeed()));
            }
            s.append(", course: ").append(String.format("%.1f", position.getCourse()));
            if (position.getAccuracy() > 0) {
                s.append(", accuracy: ").append(String.format("%.1f", position.getAccuracy()));
            }
            Object cmdResult = position.getAttributes().get(Position.KEY_RESULT);
            if (cmdResult != null) {
                s.append(", result: ").append(cmdResult);
            }
            Log.info(s.toString());

            Context.getStatisticsManager().registerMessageStored(position.getDeviceId());
        }
    }

    private static String formatChannel(Channel channel) {
        return String.format("[%s]", channel.id().asShortText());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Log.info(formatChannel(ctx.channel()) + " connected");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Log.info(formatChannel(ctx.channel()) + " disconnected");
        closeChannel(ctx.channel());

        BaseProtocolDecoder protocolDecoder = (BaseProtocolDecoder) ctx.pipeline().get("objectDecoder");
        if (ctx.pipeline().get("httpDecoder") == null
                && !connectionlessProtocols.contains(protocolDecoder.getProtocolName())) {
            Context.getConnectionManager().removeActiveDevice(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Log.warning(formatChannel(ctx.channel()) + " error", cause);
        closeChannel(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            Log.info(formatChannel(ctx.channel()) + " timed out");
            closeChannel(ctx.channel());
        }
    }

    private void closeChannel(Channel channel) {
        if (!(channel instanceof DatagramChannel)) {
            channel.close();
        }
    }

}
