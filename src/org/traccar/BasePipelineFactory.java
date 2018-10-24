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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.events.CommandResultEventHandler;
import org.traccar.events.DriverEventHandler;
import org.traccar.events.FuelDropEventHandler;
import org.traccar.events.GeofenceEventHandler;
import org.traccar.events.IgnitionEventHandler;
import org.traccar.events.MaintenanceEventHandler;
import org.traccar.events.MotionEventHandler;
import org.traccar.events.OverspeedEventHandler;
import org.traccar.events.AlertEventHandler;
import org.traccar.processing.ComputedAttributesHandler;
import org.traccar.processing.CopyAttributesHandler;

import java.net.InetSocketAddress;
import java.util.Map;

public abstract class BasePipelineFactory extends ChannelInitializer<Channel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePipelineFactory.class);

    private final TrackerServer server;
    private int timeout;

    private FilterHandler filterHandler;
    private DistanceHandler distanceHandler;
    private EngineHoursHandler engineHoursHandler;
    private RemoteAddressHandler remoteAddressHandler;
    private MotionHandler motionHandler;
    private GeocoderHandler geocoderHandler;
    private GeolocationHandler geolocationHandler;
    private HemisphereHandler hemisphereHandler;
    private CopyAttributesHandler copyAttributesHandler;
    private ComputedAttributesHandler computedAttributesHandler;

    private CommandResultEventHandler commandResultEventHandler;
    private OverspeedEventHandler overspeedEventHandler;
    private FuelDropEventHandler fuelDropEventHandler;
    private MotionEventHandler motionEventHandler;
    private GeofenceEventHandler geofenceEventHandler;
    private AlertEventHandler alertEventHandler;
    private IgnitionEventHandler ignitionEventHandler;
    private MaintenanceEventHandler maintenanceEventHandler;
    private DriverEventHandler driverEventHandler;

    private static final class OpenChannelHandler extends ChannelDuplexHandler {

        private final TrackerServer server;

        private OpenChannelHandler(TrackerServer server) {
            this.server = server;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            server.getChannelGroup().add(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            server.getChannelGroup().remove(ctx.channel());
        }

    }

    private static class NetworkMessageHandler extends ChannelDuplexHandler {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (ctx.channel() instanceof DatagramChannel) {
                DatagramPacket packet = (DatagramPacket) msg;
                ctx.fireChannelRead(new NetworkMessage(packet.content(), packet.sender()));
            } else {
                ByteBuf buffer = (ByteBuf) msg;
                ctx.fireChannelRead(new NetworkMessage(buffer, ctx.channel().remoteAddress()));
            }
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            NetworkMessage message = (NetworkMessage) msg;
            if (ctx.channel() instanceof DatagramChannel) {
                InetSocketAddress recipient = (InetSocketAddress) message.getRemoteAddress();
                InetSocketAddress sender = (InetSocketAddress) ctx.channel().localAddress();
                ctx.write(new DatagramPacket((ByteBuf) message.getMessage(), recipient, sender), promise);
            } else {
                ctx.write(message.getMessage(), promise);
            }
        }

    }

    private static class StandardLoggingHandler extends ChannelDuplexHandler {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            log(ctx, false, msg);
            super.channelRead(ctx, msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            log(ctx, true, msg);
            super.write(ctx, msg, promise);
        }

        public void log(ChannelHandlerContext ctx, boolean downstream, Object o) {
            NetworkMessage networkMessage = (NetworkMessage) o;
            StringBuilder message = new StringBuilder();

            message.append("[").append(ctx.channel().id().asShortText()).append(": ");
            message.append(((InetSocketAddress) ctx.channel().localAddress()).getPort());
            if (downstream) {
                message.append(" > ");
            } else {
                message.append(" < ");
            }

            if (networkMessage.getRemoteAddress() != null) {
                message.append(((InetSocketAddress) networkMessage.getRemoteAddress()).getHostString());
            } else {
                message.append("null");
            }
            message.append("]");

            message.append(" HEX: ");
            message.append(ByteBufUtil.hexDump((ByteBuf) networkMessage.getMessage()));

            LOGGER.info(message.toString());
        }

    }

    public BasePipelineFactory(TrackerServer server, String protocol) {
        this.server = server;

        timeout = Context.getConfig().getInteger(protocol + ".timeout");
        if (timeout == 0) {
            timeout = Context.getConfig().getInteger(protocol + ".resetDelay"); // temporary
            if (timeout == 0) {
                timeout = Context.getConfig().getInteger("server.timeout");
            }
        }

        distanceHandler = new DistanceHandler(
                Context.getConfig().getBoolean("coordinates.filter"),
                Context.getConfig().getInteger("coordinates.minError"),
                Context.getConfig().getInteger("coordinates.maxError"));

        if (Context.getConfig().getBoolean("processing.remoteAddress.enable")) {
            remoteAddressHandler = new RemoteAddressHandler();
        }

        if (Context.getConfig().getBoolean("filter.enable")) {
            filterHandler = new FilterHandler();
        }

        if (Context.getGeocoder() != null && !Context.getConfig().getBoolean("geocoder.ignorePositions")) {
            geocoderHandler = new GeocoderHandler(
                    Context.getGeocoder(),
                    Context.getConfig().getBoolean("geocoder.processInvalidPositions"));
        }

        if (Context.getGeolocationProvider() != null) {
            geolocationHandler = new GeolocationHandler(
                    Context.getGeolocationProvider(),
                    Context.getConfig().getBoolean("geolocation.processInvalidPositions"));
        }

        motionHandler = new MotionHandler(Context.getTripsConfig().getSpeedThreshold());

        if (Context.getConfig().getBoolean("processing.engineHours.enable")) {
            engineHoursHandler = new EngineHoursHandler();
        }

        if (Context.getConfig().hasKey("location.latitudeHemisphere")
                || Context.getConfig().hasKey("location.longitudeHemisphere")) {
            hemisphereHandler = new HemisphereHandler();
        }

        if (Context.getConfig().getBoolean("processing.copyAttributes.enable")) {
            copyAttributesHandler = new CopyAttributesHandler();
        }

        if (Context.getConfig().getBoolean("processing.computedAttributes.enable")) {
            computedAttributesHandler = new ComputedAttributesHandler();
        }

        if (Context.getConfig().getBoolean("event.enable")) {
            commandResultEventHandler = new CommandResultEventHandler();
            overspeedEventHandler = Context.getOverspeedEventHandler();
            fuelDropEventHandler = new FuelDropEventHandler();
            motionEventHandler = Context.getMotionEventHandler();
            geofenceEventHandler = new GeofenceEventHandler();
            alertEventHandler = new AlertEventHandler();
            ignitionEventHandler = new IgnitionEventHandler();
            maintenanceEventHandler = new MaintenanceEventHandler();
            driverEventHandler = new DriverEventHandler();
        }
    }

    protected abstract void addProtocolHandlers(PipelineBuilder pipeline);

    private void addHandlers(ChannelPipeline pipeline, ChannelHandler... handlers) {
        for (ChannelHandler handler : handlers) {
            if (handler != null) {
                pipeline.addLast(handler);
            }
        }
    }

    public static <T extends ChannelHandler> T getHandler(ChannelPipeline pipeline, Class<T> clazz) {
        for (Map.Entry<String, ChannelHandler> handlerEntry : pipeline) {
            ChannelHandler handler = handlerEntry.getValue();
            if (handler instanceof WrapperInboundHandler) {
                handler = ((WrapperInboundHandler) handler).getWrappedHandler();
            } else if (handler instanceof WrapperOutboundHandler) {
                handler = ((WrapperOutboundHandler) handler).getWrappedHandler();
            }
            if (clazz.isAssignableFrom(handler.getClass())) {
                return (T) handler;
            }
        }
        return null;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        final ChannelPipeline pipeline = channel.pipeline();
        if (timeout > 0 && !server.isDatagram()) {
            pipeline.addLast(new IdleStateHandler(timeout, 0, 0));
        }
        pipeline.addLast(new OpenChannelHandler(server));
        pipeline.addLast(new NetworkMessageHandler());
        pipeline.addLast(new StandardLoggingHandler());

        addProtocolHandlers(new PipelineBuilder() {
            @Override
            public void addLast(ChannelHandler handler) {
                if (!(handler instanceof BaseProtocolDecoder || handler instanceof BaseProtocolEncoder)) {
                    if (handler instanceof ChannelInboundHandler) {
                        handler = new WrapperInboundHandler((ChannelInboundHandler) handler);
                    } else {
                        handler = new WrapperOutboundHandler((ChannelOutboundHandler) handler);
                    }
                }
                pipeline.addLast(handler);
            }
        });

        addHandlers(
                pipeline,
                geolocationHandler,
                hemisphereHandler,
                distanceHandler,
                remoteAddressHandler);

        addDynamicHandlers(pipeline);

        addHandlers(
                pipeline,
                filterHandler,
                geocoderHandler,
                motionHandler,
                engineHoursHandler,
                copyAttributesHandler,
                computedAttributesHandler);

        if (Context.getDataManager() != null) {
            pipeline.addLast(new DefaultDataHandler());
        }

        if (Context.getConfig().getBoolean("forward.enable")) {
            pipeline.addLast(Main.getInjector().getInstance(WebDataHandler.Factory.class).create(
                    Context.getConfig().getString("forward.url"), Context.getConfig().getBoolean("forward.json")));
        }

        addHandlers(
                pipeline,
                commandResultEventHandler,
                overspeedEventHandler,
                fuelDropEventHandler,
                motionEventHandler,
                geofenceEventHandler,
                alertEventHandler,
                ignitionEventHandler,
                maintenanceEventHandler,
                driverEventHandler);

        pipeline.addLast(new MainEventHandler());
    }

    private void addDynamicHandlers(ChannelPipeline pipeline) {
        if (Context.getConfig().hasKey("extra.handlers")) {
            String[] handlers = Context.getConfig().getString("extra.handlers").split(",");
            for (String handler : handlers) {
                try {
                    pipeline.addLast((ChannelHandler) Class.forName(handler).newInstance());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException error) {
                    LOGGER.warn("Dynamic handler error", error);
                }
            }
        }
    }

}
