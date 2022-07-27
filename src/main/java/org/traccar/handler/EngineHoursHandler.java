/*
 * Copyright 2018 - 2019 Anton Tananaev (anton@traccar.org)
 * Copyright 2018 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.handler;

import io.netty.channel.ChannelHandler;
import org.traccar.BaseDataHandler;
import org.traccar.config.Config;
import org.traccar.database.IdentityManager;
import org.traccar.model.Position;

@ChannelHandler.Sharable
public class EngineHoursHandler extends BaseDataHandler {

    private final IdentityManager identityManager;
    private final double speedThreshold;

    public EngineHoursHandler(Config config, IdentityManager identityManager) {
        this.identityManager = identityManager;
        speedThreshold = config.getDouble("event.motion.speedThreshold", 0.01);
    }

    @Override
    protected Position handlePosition(Position position) {
        if (!position.getAttributes().containsKey(Position.KEY_HOURS)) {
            Position last = identityManager.getLastPosition(position.getDeviceId());
            if (last != null) {
                long hours = last.getLong(Position.KEY_HOURS);
                long idleTime = last.getLong(Position.KEY_IDLE_TIME);
                long tripTime = last.getLong(Position.KEY_TRIP_TIME);
                if (last.getBoolean(Position.KEY_IGNITION) && position.getBoolean(Position.KEY_IGNITION)) {
                    long diff = position.getFixTime().getTime() - last.getFixTime().getTime();
                    hours += diff;
                    tripTime += diff;
                    if (position.getSpeed() < speedThreshold) {
                        idleTime += diff;
                    } else {
                        idleTime = 0;
                    }
                } else {
                    idleTime = 0;
                    tripTime = 0;
                }
                if (hours != 0) {
                    position.set(Position.KEY_HOURS, hours);
                }
                position.set(Position.KEY_IDLE_TIME, idleTime);
                position.set(Position.KEY_TRIP_TIME, tripTime);
            }
        }
        return position;
    }

}
