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

import org.jboss.netty.channel.*;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.traccar.model.DataManager;

/**
  * Generic pipeline factory
  */
public abstract class GenericPipelineFactory implements ChannelPipelineFactory {

    private TrackerServer server;
    private DataManager dataManager;
    private Boolean loggerEnabled;

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
    
    public GenericPipelineFactory(
            TrackerServer server, DataManager dataManager, Boolean loggerEnabled) {
        this.server = server;
        this.dataManager = dataManager;
        this.loggerEnabled = loggerEnabled;
    }
    
    protected DataManager getDataManager() {
        return dataManager;
    }

    protected abstract void addSpecificHandlers(ChannelPipeline pipeline);
    
    public ChannelPipeline getPipeline() {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("openHandler", new OpenChannelHandler(server));
        if (loggerEnabled) {
            pipeline.addLast("logger", new LoggingHandler("logger"));
        }
        addSpecificHandlers(pipeline);
        //pipeline.addLast("frameDecoder", new XexunFrameDecoder());
        //pipeline.addLast("stringDecoder", new StringDecoder());
        //pipeline.addLast("objectDecoder", new XexunProtocolDecoder(serverCreator, resetDelay));
        pipeline.addLast("handler", new TrackerEventHandler(dataManager));            
        return pipeline;
    }
}