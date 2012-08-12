/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.traccar.geocode.ReverseGeocoder;
import org.traccar.helper.Log;
import org.traccar.model.DataManager;

/**
  * Generic pipeline factory
  */
public abstract class GenericPipelineFactory implements ChannelPipelineFactory {

    private TrackerServer server;
    private DataManager dataManager;
    private Boolean loggerEnabled;
    private ReverseGeocoder geocoder;

    /**
     * Open channel handler
     */
    protected class OpenChannelHandler extends SimpleChannelHandler {

        private TrackerServer server;

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

        static final String HEX_CHARS = "0123456789ABCDEF";

        @Override
        public void log(ChannelEvent e) {
        	
            if (e instanceof MessageEvent) {
                MessageEvent event = (MessageEvent) e;
                String msg = "[" + ((InetSocketAddress) e.getChannel().getLocalAddress()).getPort() + " - ";
                msg += ((InetSocketAddress) event.getRemoteAddress()).getAddress().getHostAddress() +  "]";

                // Append hex message
                if (event.getMessage() instanceof ChannelBuffer) {
                    ChannelBuffer buffer = (ChannelBuffer) event.getMessage();
                    msg += " - (HEX: ";
                    for (int i = buffer.readerIndex(); i < buffer.writerIndex(); i++) {
                        byte b = buffer.getByte(i);
                        msg += HEX_CHARS.charAt((b & 0xf0) >> 4);
                        msg += HEX_CHARS.charAt((b & 0x0F));
                    }
                    msg += ")";
                }

                Log.fine(msg);
            } else if (e instanceof ExceptionEvent) {
                ExceptionEvent event = (ExceptionEvent) e;
                Log.warning(event.getCause().getMessage());
            }
            // TODO: handle other events
        }
    }

    public GenericPipelineFactory(
            TrackerServer server, DataManager dataManager, Boolean loggerEnabled, ReverseGeocoder geocoder) {
        this.server = server;
        this.dataManager = dataManager;
        this.loggerEnabled = loggerEnabled;
        this.geocoder = geocoder;
    }

    protected DataManager getDataManager() {
        return dataManager;
    }

    protected abstract void addSpecificHandlers(ChannelPipeline pipeline);

    public ChannelPipeline getPipeline() {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("openHandler", new OpenChannelHandler(server));
        if (loggerEnabled) {
            pipeline.addLast("logger", new StandardLoggingHandler());
        }
        addSpecificHandlers(pipeline);
        if (geocoder != null) {
            pipeline.addLast("geocoder", new ReverseGeocoderHandler(geocoder));
        }
        pipeline.addLast("handler", new TrackerEventHandler(dataManager));
       
        return pipeline;
    }
}