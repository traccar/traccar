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
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Keys;
import org.traccar.handler.events.AlertEventHandler;
import org.traccar.handler.events.CommandResultEventHandler;
import org.traccar.handler.events.DriverEventHandler;
import org.traccar.handler.events.FuelDropEventHandler;
import org.traccar.handler.events.GeofenceEventHandler;
import org.traccar.handler.events.IgnitionEventHandler;
import org.traccar.handler.events.MaintenanceEventHandler;
import org.traccar.handler.events.MotionEventHandler;
import org.traccar.handler.events.OverspeedEventHandler;
import org.traccar.handler.ComputedAttributesHandler;
import org.traccar.handler.CopyAttributesHandler;
import org.traccar.handler.DistanceHandler;
import org.traccar.handler.EngineHoursHandler;
import org.traccar.handler.FilterHandler;
import org.traccar.handler.GeocoderHandler;
import org.traccar.handler.GeolocationHandler;
import org.traccar.handler.HemisphereHandler;
import org.traccar.handler.MotionHandler;
import org.traccar.handler.NetworkMessageHandler;
import org.traccar.handler.OpenChannelHandler;
import org.traccar.handler.RemoteAddressHandler;
import org.traccar.handler.StandardLoggingHandler;

import java.util.Map;

public abstract class BasePipelineFactory extends ChannelInitializer<Channel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePipelineFactory.class);

    private final TrackerServer server;
    private boolean eventsEnabled;
    private int timeout;

    private IgnitionEventHandler ignitionEventHandler;
    private MaintenanceEventHandler maintenanceEventHandler;
    private DriverEventHandler driverEventHandler;

    public BasePipelineFactory(TrackerServer server, String protocol) {
        this.server = server;
        eventsEnabled = Context.getConfig().getBoolean(Keys.EVENT_ENABLE);
        timeout = Context.getConfig().getInteger(Keys.PROTOCOL_TIMEOUT.withPrefix(protocol));
        if (timeout == 0) {
            timeout = Context.getConfig().getInteger(Keys.SERVER_TIMEOUT);
        }

        if (eventsEnabled) {
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
    protected void initChannel(Channel channel) {
        final ChannelPipeline pipeline = channel.pipeline();

        if (timeout > 0 && !server.isDatagram()) {
            pipeline.addLast(new IdleStateHandler(timeout, 0, 0));
        }
        pipeline.addLast(new OpenChannelHandler(server));
        pipeline.addLast(new NetworkMessageHandler());
        pipeline.addLast(new StandardLoggingHandler());

        addProtocolHandlers(handler -> {
            if (!(handler instanceof BaseProtocolDecoder || handler instanceof BaseProtocolEncoder)) {
                if (handler instanceof ChannelInboundHandler) {
                    handler = new WrapperInboundHandler((ChannelInboundHandler) handler);
                } else {
                    handler = new WrapperOutboundHandler((ChannelOutboundHandler) handler);
                }
            }
            pipeline.addLast(handler);
        });

        addHandlers(
                pipeline,
                Main.getInjector().getInstance(GeolocationHandler.class),
                Main.getInjector().getInstance(HemisphereHandler.class),
                Main.getInjector().getInstance(DistanceHandler.class),
                Main.getInjector().getInstance(RemoteAddressHandler.class));

        addDynamicHandlers(pipeline);

        addHandlers(
                pipeline,
                Main.getInjector().getInstance(FilterHandler.class),
                Main.getInjector().getInstance(GeocoderHandler.class),
                Main.getInjector().getInstance(MotionHandler.class),
                Main.getInjector().getInstance(EngineHoursHandler.class),
                Main.getInjector().getInstance(CopyAttributesHandler.class),
                Main.getInjector().getInstance(ComputedAttributesHandler.class),
                Main.getInjector().getInstance(WebDataHandler.class));

        if (Context.getDataManager() != null) {
            pipeline.addLast(new DefaultDataHandler());
        }

        if (eventsEnabled) {
            addHandlers(
                    pipeline,
                    Main.getInjector().getInstance(CommandResultEventHandler.class),
                    Main.getInjector().getInstance(OverspeedEventHandler.class),
                    Main.getInjector().getInstance(FuelDropEventHandler.class),
                    Main.getInjector().getInstance(MotionEventHandler.class),
                    Main.getInjector().getInstance(GeofenceEventHandler.class),
                    Main.getInjector().getInstance(AlertEventHandler.class),
                    ignitionEventHandler,
                    maintenanceEventHandler,
                    driverEventHandler);
        }

        pipeline.addLast(new MainEventHandler());
    }

    private void addDynamicHandlers(ChannelPipeline pipeline) {
        String handlers = Context.getConfig().getString(Keys.EXTRA_HANDLERS);
        if (handlers != null) {
            for (String handler : handlers.split(",")) {
                try {
                    pipeline.addLast((ChannelHandler) Class.forName(handler).getDeclaredConstructor().newInstance());
                } catch (ReflectiveOperationException error) {
                    LOGGER.warn("Dynamic handler error", error);
                }
            }
        }
    }

}
