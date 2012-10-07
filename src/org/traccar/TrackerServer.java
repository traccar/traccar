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
import java.nio.ByteOrder;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * Tracker server
 */
public class TrackerServer extends ServerBootstrap {

    /**
     * Initialization
     */
    private void init(Integer port, Integer threadPoolSize) {
        
        setPort(port);

        // Create channel factory
        setFactory(new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));
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
     * Set endianness
     */
    void setEndianness(ByteOrder byteOrder) {
        setOption("child.bufferFactory", new HeapChannelBufferFactory(byteOrder));
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
