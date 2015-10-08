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

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public class MainEventHandler extends IdleStateAwareChannelHandler {

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

        if (e.getMessage() != null && e.getMessage() instanceof Position) {

            Position position = (Position) e.getMessage();

            // Log position
            StringBuilder s = new StringBuilder();
            s.append(formatChannel(e.getChannel())).append(" ");
            s.append("id: ").append(position.getDeviceId()).append(", ");
            s.append("time: ").append(position.getFixTime()).append(", ");
            s.append("lat: ").append(position.getLatitude()).append(", ");
            s.append("lon: ").append(position.getLongitude()).append(", ");
            s.append("speed: ").append(position.getSpeed()).append(", ");
            s.append("course: ").append(position.getCourse());
            Log.info(s.toString());

            Context.getConnectionManager().update(position);
        }
    }

    private static String formatChannel(Channel channel) {
        return String.format("[%08X]", channel.getId());
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        Log.info(formatChannel(e.getChannel()) + " connected");
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        Log.info(formatChannel(e.getChannel()) + " disconnected");
        e.getChannel().close();

        Context.getConnectionManager().removeActiveDevice(e.getChannel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        Log.warning(formatChannel(e.getChannel()) + " error", e.getCause());
        e.getChannel().close();
    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) {
        Log.info(formatChannel(e.getChannel()) + " timed out");
        e.getChannel().close();
    }

}
