/*
 * Copyright 2012 - 2019 Anton Tananaev (anton@traccar.org)
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
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.helper.DateUtil;
import org.traccar.model.Position;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainEventHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeocoderHandler.class);
    private static final String DEFAULT_LOGGER_EVENTS = "time,position,speed,course,accuracy,result";

    private final Set<String> connectionlessProtocols = new HashSet<>();
    private final Set<String> logEvents;

    public MainEventHandler() {
        String connectionlessProtocolList = Context.getConfig().getString("status.ignoreOffline");
        if (connectionlessProtocolList != null) {
            connectionlessProtocols.addAll(Arrays.asList(connectionlessProtocolList.split(",")));
        }
        logEvents = new HashSet<>(Arrays.asList(Context.getConfig().getString("logger.events", DEFAULT_LOGGER_EVENTS).split(",")));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Position) {

            Position position = (Position) msg;
            try {
                Context.getDeviceManager().updateLatestPosition(position);
            } catch (SQLException error) {
                LOGGER.warn("Failed to update device", error);
            }

            String uniqueId = Context.getIdentityManager().getById(position.getDeviceId()).getUniqueId();

            // Log position
            StringBuilder s = new StringBuilder();
            s.append(formatChannel(ctx.channel())).append(" ");
            s.append("id: ").append(uniqueId);
            if (logEvents.contains("time")) {
                s.append(", time: ").append(DateUtil.formatDate(position.getFixTime(), false));
            }
            if (logEvents.contains("position")) {
                s.append(", lat: ").append(String.format("%.5f", position.getLatitude()));
                s.append(", lon: ").append(String.format("%.5f", position.getLongitude()));
            }
            if (position.getSpeed() > 0 && logEvents.contains("speed")) {
                s.append(", speed: ").append(String.format("%.1f", position.getSpeed()));
            }
            if (logEvents.contains("course")) {
                s.append(", course: ").append(String.format("%.1f", position.getCourse()));
            }
            if (position.getAccuracy() > 0 && logEvents.contains("accuracy")) {
                s.append(", accuracy: ").append(String.format("%.1f", position.getAccuracy()));
            }
            if (position.getOutdated() && logEvents.contains("outdated")) {
                s.append(", outdated");
            }
            if (!position.getValid() && logEvents.contains("invalid")) {
                s.append(", invalid");
            }
            if (logEvents.contains(Position.KEY_BATTERY_LEVEL)) {
                Object batteryLevel = position.getInteger(Position.KEY_BATTERY_LEVEL, null);
                if (batteryLevel != null) {
                    s.append(", battery: ").append(batteryLevel).append('%');
                }
            }
            if (logEvents.contains(Position.KEY_RESULT)) {
                Object cmdResult = position.getAttributes().get(Position.KEY_RESULT);
                if (cmdResult != null) {
                    s.append(", result: ").append(cmdResult);
                }
            }
            LOGGER.info(s.toString());

            Context.getStatisticsManager().registerMessageStored(position.getDeviceId());
        }
    }

    private static String formatChannel(Channel channel) {
        return String.format("[%s]", channel.id().asShortText());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (!(ctx.channel() instanceof DatagramChannel)) {
            LOGGER.info(formatChannel(ctx.channel()) + " connected");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info(formatChannel(ctx.channel()) + " disconnected");
        closeChannel(ctx.channel());

        if (BasePipelineFactory.getHandler(ctx.pipeline(), HttpRequestDecoder.class) == null
                && !connectionlessProtocols.contains(ctx.pipeline().get(BaseProtocolDecoder.class).getProtocolName())) {
            Context.getConnectionManager().removeActiveDevice(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        LOGGER.warn(formatChannel(ctx.channel()) + " error", cause);
        closeChannel(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            LOGGER.info(formatChannel(ctx.channel()) + " timed out");
            closeChannel(ctx.channel());
        }
    }

    private void closeChannel(Channel channel) {
        if (!(channel instanceof DatagramChannel)) {
            channel.close();
        }
    }

}
