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

import java.util.List;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.traccar.database.DataCache;
import org.traccar.helper.Log;
import org.traccar.database.DataManager;
import org.traccar.model.Event;
import org.traccar.model.Position;

@ChannelHandler.Sharable
public class TrackerEventHandler extends IdleStateAwareChannelHandler {

    private Long processSinglePosition(Position position) {
        if (position == null) {
            Log.info("processSinglePosition null message");
        } else {
            StringBuilder s = new StringBuilder();
            s.append("device: ").append(position.getDeviceId()).append(", ");
            s.append("time: ").append(position.getFixTime()).append(", ");
            s.append("lat: ").append(position.getLatitude()).append(", ");
            s.append("lon: ").append(position.getLongitude());
            Log.info(s.toString());
        }

        // Write position to database
        Long id = null;
        try {
            id = Context.getDataManager().addPosition(position);
        } catch (Exception error) {
            Log.warning(error);
        }
        return id;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        Long id = null;
        Position lastPostition = null;
        if (e.getMessage() instanceof Position) {
            id = processSinglePosition((Position) e.getMessage());
            lastPostition = (Position) e.getMessage();
        } else if (e.getMessage() instanceof List) {
            List<Position> positions = (List<Position>) e.getMessage();
            for (Position position : positions) {
                id = processSinglePosition(position);
                lastPostition = position;
            }
        }
        if (lastPostition != null) {
            try {
                Context.getDataManager().updateLatestPosition(lastPostition, id);
                Context.getDataCache().update(lastPostition);
            } catch (Exception error) {
                Log.warning(error);
            }
        }
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        Log.info("Closing connection by disconnect");
        e.getChannel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        Log.info("Closing connection by exception");
        e.getChannel().close();
    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) {
        Log.info("Closing connection by timeout");
        e.getChannel().close();
    }

}
