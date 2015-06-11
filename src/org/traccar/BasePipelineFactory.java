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
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.traccar.database.DataCache;
import org.traccar.database.DataManager;
import org.traccar.geocode.ReverseGeocoder;
import org.traccar.helper.Log;

import javax.crypto.Cipher;

public abstract class BasePipelineFactory implements ChannelPipelineFactory {

    private final TrackerServer server;
    private FilterHandler filterHandler;
    private Integer resetDelay;
    private Boolean processInvalidPositions;

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

    /**
     * Logging using global logger
     */
    protected class StandardLoggingHandler extends LoggingHandler {

        @Override
        public void log(ChannelEvent e) {
            if (e instanceof MessageEvent) {
                MessageEvent event = (MessageEvent) e;
                StringBuilder msg = new StringBuilder();

                msg.append("[").append(String.format("%08X", e.getChannel().getId())).append(": ");
                msg.append(((InetSocketAddress) e.getChannel().getLocalAddress()).getPort());
                msg.append((e instanceof DownstreamMessageEvent) ? " -> " : " <- ");

                msg.append(((InetSocketAddress) event.getRemoteAddress()).getAddress().getHostAddress()).append("]");

                // Append hex message
                if (event.getMessage() instanceof ChannelBuffer) {
                    msg.append(" - HEX: ");
                    msg.append(ChannelBuffers.hexDump((ChannelBuffer) event.getMessage()));
                }

                Log.debug(msg.toString());
            } else if (e instanceof ExceptionEvent) {
                ExceptionEvent event = (ExceptionEvent) e;
                Log.warning(event.getCause());
            }
        }

    }

    public BasePipelineFactory(TrackerServer server, String protocol) {
        this.server = server;

        String resetDelayProperty = Context.getProps().getProperty(protocol + ".resetDelay");
        if (resetDelayProperty != null) {
            resetDelay = Integer.valueOf(resetDelayProperty);
        }

        String enableFilter = Context.getProps().getProperty("filter.enable");
        if (enableFilter != null && Boolean.valueOf(enableFilter)) {
            filterHandler = new FilterHandler();
        }

        if (Context.getReverseGeocoder() != null) {
            // Default behavior is to process invalid positions (i.e., the "null" case)
            String invalidPositions = Context.getProps().getProperty("geocode.processInvalidPositions");
            processInvalidPositions = (invalidPositions == null || Boolean.valueOf(invalidPositions));
        }
    }

    protected abstract void addSpecificHandlers(ChannelPipeline pipeline);

    @Override
    public ChannelPipeline getPipeline() {
        ChannelPipeline pipeline = Channels.pipeline();
        if (resetDelay != null) {
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
        if (Context.getReverseGeocoder() != null) {
            pipeline.addLast("geocoder", new ReverseGeocoderHandler(Context.getReverseGeocoder(), processInvalidPositions));
        }
        pipeline.addLast("handler", new TrackerEventHandler());
        return pipeline;
    }

}
