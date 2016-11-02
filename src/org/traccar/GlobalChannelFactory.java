/*
 * Copyright 2012 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public final class GlobalChannelFactory {

    private static ChannelFactory channelFactory = null;
    private static DatagramChannelFactory datagramChannelFactory = null;

    private GlobalChannelFactory() {
    }

    public static void release() {
        if (channelFactory != null) {
            channelFactory.releaseExternalResources();
        }
        if (datagramChannelFactory != null) {
            datagramChannelFactory.releaseExternalResources();
        }
        channelFactory = null;
        datagramChannelFactory = null;
    }

    public static ChannelFactory getFactory() {
        if (channelFactory == null) {
            channelFactory = new NioServerSocketChannelFactory();
        }
        return channelFactory;
    }

    public static DatagramChannelFactory getDatagramFactory() {
        if (datagramChannelFactory == null) {
            datagramChannelFactory = new NioDatagramChannelFactory();
        }
        return datagramChannelFactory;
    }

}
