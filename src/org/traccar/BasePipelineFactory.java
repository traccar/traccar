/*
 * Copyright 2012 - 2017 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.traccar.events.CommandResultEventHandler;
import org.traccar.events.FuelDropEventHandler;
import org.traccar.events.GeofenceEventHandler;
import org.traccar.events.IgnitionEventHandler;
import org.traccar.events.MaintenanceEventHandler;
import org.traccar.events.MotionEventHandler;
import org.traccar.events.OverspeedEventHandler;
import org.traccar.events.AlertEventHandler;
import org.traccar.helper.Log;
import org.traccar.processing.ComputedAttributesHandler;
import org.traccar.processing.CopyAttributesHandler;

import java.net.InetSocketAddress;

public abstract class BasePipelineFactory implements ChannelPipelineFactory {

    private final TrackerServer server;
    private int timeout;

    private FilterHandler filterHandler;
    private CoordinatesHandler coordinatesHandler;
    private DistanceHandler distanceHandler;
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

    private static final class OpenChannelHandler extends SimpleChannelHandler {

        private final TrackerServer server;

        private OpenChannelHandler(TrackerServer server) {
            this.server = server;
        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
            server.getChannelGroup().add(e.getChannel());
        }
    }

    private static class StandardLoggingHandler extends LoggingHandler {

        @Override
        public void log(ChannelEvent e) {
            if (e instanceof MessageEvent) {
                MessageEvent event = (MessageEvent) e;
                StringBuilder msg = new StringBuilder();

                msg.append("[").append(String.format("%08X", e.getChannel().getId())).append(": ");
                msg.append(((InetSocketAddress) e.getChannel().getLocalAddress()).getPort());
                if (e instanceof DownstreamMessageEvent) {
                    msg.append(" > ");
                } else {
                    msg.append(" < ");
                }

                if (event.getRemoteAddress() != null) {
                    msg.append(((InetSocketAddress) event.getRemoteAddress()).getHostString());
                } else {
                    msg.append("null");
                }
                msg.append("]");

                if (event.getMessage() instanceof ChannelBuffer) {
                    msg.append(" HEX: ");
                    msg.append(ChannelBuffers.hexDump((ChannelBuffer) event.getMessage()));
                }

                Log.debug(msg.toString());
            }
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

        if (Context.getConfig().getBoolean("filter.enable")) {
            filterHandler = new FilterHandler();
        }

        if (Context.getConfig().getBoolean("coordinates.filter")) {
            coordinatesHandler = new CoordinatesHandler();
        }

        if (Context.getGeocoder() != null) {
            geocoderHandler = new GeocoderHandler(
                    Context.getGeocoder(),
                    Context.getConfig().getBoolean("geocoder.processInvalidPositions"));
        }

        if (Context.getGeolocationProvider() != null) {
            geolocationHandler = new GeolocationHandler(
                    Context.getGeolocationProvider(),
                    Context.getConfig().getBoolean("geolocation.processInvalidPositions"));
        }

        distanceHandler = new DistanceHandler();

        motionHandler = new MotionHandler(Context.getConfig().getDouble("event.motion.speedThreshold", 0.01));

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
            overspeedEventHandler = new OverspeedEventHandler();
            fuelDropEventHandler = new FuelDropEventHandler();
            motionEventHandler = new MotionEventHandler();
            geofenceEventHandler = new GeofenceEventHandler();
            alertEventHandler = new AlertEventHandler();
            ignitionEventHandler = new IgnitionEventHandler();
            maintenanceEventHandler = new MaintenanceEventHandler();
        }
    }

    protected abstract void addSpecificHandlers(ChannelPipeline pipeline);

    @Override
    public ChannelPipeline getPipeline() {
        ChannelPipeline pipeline = Channels.pipeline();
        if (timeout > 0 && !server.isConnectionless()) {
            pipeline.addLast("idleHandler", new IdleStateHandler(GlobalTimer.getTimer(), timeout, 0, 0));
        }
        pipeline.addLast("openHandler", new OpenChannelHandler(server));
        if (Context.isLoggerEnabled()) {
            pipeline.addLast("logger", new StandardLoggingHandler());
        }

        addSpecificHandlers(pipeline);

        if (geolocationHandler != null) {
            pipeline.addLast("location", geolocationHandler);
        }
        if (hemisphereHandler != null) {
            pipeline.addLast("hemisphere", hemisphereHandler);
        }
        if (geocoderHandler != null) {
            pipeline.addLast("geocoder", geocoderHandler);
        }
        pipeline.addLast("remoteAddress", new RemoteAddressHandler());

        addDynamicHandlers(pipeline);

        if (filterHandler != null) {
            pipeline.addLast("filter", filterHandler);
        }

        if (coordinatesHandler != null) {
            pipeline.addLast("coordinatesHandler", coordinatesHandler);
        }

        if (distanceHandler != null) {
            pipeline.addLast("distance", distanceHandler);
        }

        if (motionHandler != null) {
            pipeline.addLast("motion", motionHandler);
        }

        if (copyAttributesHandler != null) {
            pipeline.addLast("copyAttributes", copyAttributesHandler);
        }

        if (computedAttributesHandler != null) {
            pipeline.addLast("computedAttributes", computedAttributesHandler);
        }

        if (Context.getDataManager() != null) {
            pipeline.addLast("dataHandler", new DefaultDataHandler());
        }

        if (Context.getConfig().getBoolean("forward.enable")) {
            pipeline.addLast("webHandler", new WebDataHandler(Context.getConfig().getString("forward.url")));
        }

        if (commandResultEventHandler != null) {
            pipeline.addLast("CommandResultEventHandler", commandResultEventHandler);
        }

        if (overspeedEventHandler != null) {
            pipeline.addLast("OverspeedEventHandler", overspeedEventHandler);
        }

        if (fuelDropEventHandler != null) {
            pipeline.addLast("FuelDropEventHandler", fuelDropEventHandler);
        }

        if (motionEventHandler != null) {
            pipeline.addLast("MotionEventHandler", motionEventHandler);
        }

        if (geofenceEventHandler != null) {
            pipeline.addLast("GeofenceEventHandler", geofenceEventHandler);
        }

        if (alertEventHandler != null) {
            pipeline.addLast("AlertEventHandler", alertEventHandler);
        }

        if (ignitionEventHandler != null) {
            pipeline.addLast("IgnitionEventHandler", ignitionEventHandler);
        }

        if (maintenanceEventHandler != null) {
            pipeline.addLast("MaintenanceEventHandler", maintenanceEventHandler);
        }

        pipeline.addLast("mainHandler", new MainEventHandler());
        return pipeline;
    }

    private void addDynamicHandlers(ChannelPipeline pipeline) {
        if (Context.getConfig().hasKey("extra.handlers")) {
            String[] handlers = Context.getConfig().getString("extra.handlers").split(",");
            for (int i = 0; i < handlers.length; i++) {
                try {
                    pipeline.addLast("extraHandler." + i, (ChannelHandler) Class.forName(handlers[i]).newInstance());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException error) {
                    Log.warning(error);
                }
            }
        }
    }
}
