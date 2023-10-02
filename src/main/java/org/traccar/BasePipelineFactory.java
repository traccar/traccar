/*
 * Copyright 2012 - 2023 Anton Tananaev (anton@traccar.org)
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

import com.google.inject.Injector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.handler.AcknowledgementHandler;
import org.traccar.handler.ComputedAttributesHandler;
import org.traccar.handler.CopyAttributesHandler;
import org.traccar.handler.DefaultDataHandler;
import org.traccar.handler.DistanceHandler;
import org.traccar.handler.EngineHoursHandler;
import org.traccar.handler.FilterHandler;
import org.traccar.handler.GeocoderHandler;
import org.traccar.handler.GeofenceHandler;
import org.traccar.handler.GeolocationHandler;
import org.traccar.handler.HemisphereHandler;
import org.traccar.handler.MotionHandler;
import org.traccar.handler.NetworkForwarderHandler;
import org.traccar.handler.NetworkMessageHandler;
import org.traccar.handler.OpenChannelHandler;
import org.traccar.handler.RemoteAddressHandler;
import org.traccar.handler.SpeedLimitHandler;
import org.traccar.handler.StandardLoggingHandler;
import org.traccar.handler.TimeHandler;
import org.traccar.handler.events.AlertEventHandler;
import org.traccar.handler.events.BehaviorEventHandler;
import org.traccar.handler.events.CommandResultEventHandler;
import org.traccar.handler.events.DriverEventHandler;
import org.traccar.handler.events.FuelEventHandler;
import org.traccar.handler.events.GeofenceEventHandler;
import org.traccar.handler.events.IgnitionEventHandler;
import org.traccar.handler.events.MaintenanceEventHandler;
import org.traccar.handler.events.MediaEventHandler;
import org.traccar.handler.events.MotionEventHandler;
import org.traccar.handler.events.OverspeedEventHandler;

import java.util.Map;

public abstract class BasePipelineFactory extends ChannelInitializer<Channel> {

    private final Injector injector;
    private final TrackerConnector connector;
    private final Config config;
    private final String protocol;
    private final int timeout;

    public BasePipelineFactory(TrackerConnector connector, Config config, String protocol) {
        this.injector = Main.getInjector();
        this.connector = connector;
        this.config = config;
        this.protocol = protocol;
        int timeout = config.getInteger(Keys.PROTOCOL_TIMEOUT.withPrefix(protocol));
        if (timeout == 0) {
            this.timeout = config.getInteger(Keys.SERVER_TIMEOUT);
        } else {
            this.timeout = timeout;
        }
    }

    protected abstract void addTransportHandlers(PipelineBuilder pipeline);

    protected abstract void addProtocolHandlers(PipelineBuilder pipeline);

    @SafeVarargs
    private void addHandlers(ChannelPipeline pipeline, Class<? extends ChannelHandler>... handlerClasses) {
        for (Class<? extends ChannelHandler> handlerClass : handlerClasses) {
            if (handlerClass != null) {
                pipeline.addLast(injector.getInstance(handlerClass));
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

        addTransportHandlers(pipeline::addLast);

        if (timeout > 0 && !connector.isDatagram()) {
            pipeline.addLast(new IdleStateHandler(timeout, 0, 0));
        }
        pipeline.addLast(new OpenChannelHandler(connector));
        if (config.hasKey(Keys.SERVER_FORWARD)) {
            int port = config.getInteger(Keys.PROTOCOL_PORT.withPrefix(protocol));
            var handler = new NetworkForwarderHandler(port);
            injector.injectMembers(handler);
            pipeline.addLast(handler);
        }
        pipeline.addLast(new NetworkMessageHandler());
        pipeline.addLast(new StandardLoggingHandler(protocol));
        if (!connector.isDatagram() && !config.getBoolean(Keys.SERVER_INSTANT_ACKNOWLEDGEMENT)) {
            pipeline.addLast(new AcknowledgementHandler());
        }

        addProtocolHandlers(handler -> {
            if (handler instanceof BaseProtocolDecoder || handler instanceof BaseProtocolEncoder) {
                injector.injectMembers(handler);
            } else {
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
                TimeHandler.class,
                GeolocationHandler.class,
                HemisphereHandler.class,
                DistanceHandler.class,
                RemoteAddressHandler.class,
                FilterHandler.class,
                GeofenceHandler.class,
                GeocoderHandler.class,
                SpeedLimitHandler.class,
                MotionHandler.class,
                CopyAttributesHandler.class,
                EngineHoursHandler.class,
                ComputedAttributesHandler.class,
                PositionForwardingHandler.class,
                DefaultDataHandler.class,
                MediaEventHandler.class,
                CommandResultEventHandler.class,
                OverspeedEventHandler.class,
                BehaviorEventHandler.class,
                FuelEventHandler.class,
                MotionEventHandler.class,
                GeofenceEventHandler.class,
                AlertEventHandler.class,
                IgnitionEventHandler.class,
                MaintenanceEventHandler.class,
                DriverEventHandler.class,
                MainEventHandler.class);
    }

}
