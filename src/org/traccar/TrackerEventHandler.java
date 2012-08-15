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
import org.traccar.helper.Log;
import org.traccar.model.DataManager;
import org.traccar.model.Position;

/**
 * Tracker message handler
 */
@ChannelHandler.Sharable
public class TrackerEventHandler extends SimpleChannelHandler {

    /**
     * Data manager
     */
    private DataManager dataManager;

    TrackerEventHandler(DataManager newDataManager) {
        super();
        dataManager = newDataManager;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

        if (e.getMessage() instanceof Position) {

            Position position = (Position) e.getMessage();

            if (position == null) {
                Log.info("null message");
            } else {
                Log.info(
                        "id: " + position.getId() +
                        ", deviceId: " + position.getDeviceId() +
                        ", valid: " + position.getValid() +
                        ", time: " + position.getTime() +
                        ", latitude: " + position.getLatitude() +
                        ", longitude: " + position.getLongitude() +
                        ", altitude: " + position.getAltitude() +
                        ", speed: " + position.getSpeed() +
                        ", course: " + position.getCourse() +
                        ", power: " + position.getPower() +
                        ", mode: " + position.getMode() +
                        ", address: " + position.getAddress());
            }

            // Write position to database
            try {
                dataManager.addPosition(position);
            } catch (Exception error) {
                Log.warning(error.getMessage());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getChannel().close();
    }

}
