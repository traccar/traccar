/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.net.InetSocketAddress;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
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
import org.traccar.helper.Log;

public abstract class BasePipelineFactory implements ChannelPipelineFactory {

    private final TrackerServer server;
    private final int resetDelay;

    private FilterHandler filterHandler;
    private DistanceHandler distanceHandler;
    private ReverseGeocoderHandler reverseGeocoderHandler;

    protected class OpenChannelHandler extends SimpleChannelHandler {

        private final TrackerServer server;

        public OpenChannelHandler(TrackerServer server) {
            this.server = server;
        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
            server.getChannelGroup().add(e.getChannel());
        }
    }

    protected class StandardLoggingHandler extends LoggingHandler {

        @Override
        public void log(ChannelEvent e) {
            if (e instanceof MessageEvent) {
                MessageEvent event = (MessageEvent) e;
                StringBuilder msg = new StringBuilder();

                msg.append("[").append(String.format("%08X", e.getChannel().getId())).append(": ");
                msg.append(((InetSocketAddress) e.getChannel().getLocalAddress()).getPort());
                msg.append((e instanceof DownstreamMessageEvent) ? " > " : " < ");

                msg.append(((InetSocketAddress) event.getRemoteAddress()).getAddress().getHostAddress()).append("]");

                // Append hex message
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
        
        resetDelay = Context.getConfig().getInteger(protocol + ".resetDelay", 0);

        if (Context.getConfig().getBoolean("filter.enable")) {
            filterHandler = new FilterHandler();
        }
        
        if (Context.getReverseGeocoder() != null) {
            reverseGeocoderHandler = new ReverseGeocoderHandler(
                    Context.getReverseGeocoder(), Context.getConfig().getBoolean("geocode.processInvalidPositions"));
        }

        if (Context.getConfig().getBoolean("distance.enable")) {
            distanceHandler = new DistanceHandler();
        }
    }

    protected abstract void addSpecificHandlers(ChannelPipeline pipeline);

    @Override
    public ChannelPipeline getPipeline() {
        ChannelPipeline pipeline = Channels.pipeline();
        if (resetDelay != 0) {
            pipeline.addLast("idleHandler", new IdleStateHandler(GlobalTimer.getTimer(), resetDelay, 0, 0));
        }
        pipeline.addLast("openHandler", new OpenChannelHandler(server));
        if (Context.isLoggerEnabled()) {
            pipeline.addLast("logger", new StandardLoggingHandler());
        }
        addSpecificHandlers(pipeline);
        if (filterHandler != null) {
            pipeline.addLast("filter", filterHandler);
        }
        if (distanceHandler != null) {
            pipeline.addLast("distance", distanceHandler);
        }
        if (reverseGeocoderHandler != null) {
            pipeline.addLast("geocoder", reverseGeocoderHandler);
        }
        pipeline.addLast("remoteAddress", new RemoteAddressHandler());
        if (Context.getDataManager() != null) {
            pipeline.addLast("dataHandler", new DefaultDataHandler());
        }
        if (Context.getConfig().getBoolean("forward.enable")) {
            pipeline.addLast("webHandler", new WebDataHandler(Context.getConfig().getString("forward.url")));
        }
        pipeline.addLast("mainHandler", new MainEventHandler());
        return pipeline;
    }

}
