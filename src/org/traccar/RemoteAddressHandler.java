/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.traccar.geocode.AddressFormat;
import org.traccar.geocode.ReverseGeocoder;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

public class RemoteAddressHandler extends ExtendedObjectDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String hostAddress = ((InetSocketAddress) remoteAddress).getAddress().getHostAddress();
        
        if (msg instanceof Position) {
            Position position = (Position) msg;
            position.set(Event.KEY_IP, hostAddress);
        } else if (msg instanceof List) {
            List<Position> positions = (List<Position>) msg;
            for (Position position : positions) {
                position.set(Event.KEY_IP, hostAddress);
            }
        }

        return msg;
    }

}
