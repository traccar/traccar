/*
 * Copyright 2010 Anton Tananaev (anton@tananaev.com)
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
package net.sourceforge.opentracking;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelPipelineCoverage;

/**
 * Tracker server
 */
public class TrackerServer extends ServerBootstrap {

    /**
     * Open channel handler
     */
    @ChannelPipelineCoverage("all")
    protected class OpenChannelHandler extends SimpleChannelHandler {

        TrackerServer server;

        public OpenChannelHandler(TrackerServer newServer) {
            server = newServer;
        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
            //System.out.println("connected: " + e.getChannel().getRemoteAddress());
            server.getChannelGroup().add(e.getChannel());
        }
    }

    /**
     * Initialization
     */
    private void init(Integer port, Integer threadPoolSize) {
        
        setPort(port);

        // Create channel factory
        setFactory(new NioServerSocketChannelFactory(
                Executors.newFixedThreadPool(threadPoolSize),
                Executors.newFixedThreadPool(threadPoolSize)));

        // Add open channel handler
        getPipeline().addLast("openHandler", new OpenChannelHandler(this));
    }

    public TrackerServer(Integer port) {
        init(port, 1);
    }

    /**
     * Server port
     */
    private Integer port;

    public Integer getPort() {
        return port;
    }

    private void setPort(Integer newPort) {
        port = newPort;
    }

    /**
     * Opened channels
     */
    private ChannelGroup allChannels = new DefaultChannelGroup();

    public ChannelGroup getChannelGroup() {
        return allChannels;
    }
    
    /**
     * Start server
     */
    public void start() {
        Channel channel = bind(new InetSocketAddress(getPort()));
        getChannelGroup().add(channel);
    }
    
    /**
     * Stop server
     */
    public void stop() {
        ChannelGroupFuture future = getChannelGroup().close();
        future.awaitUninterruptibly();
        getFactory().releaseExternalResources();
    }

}
