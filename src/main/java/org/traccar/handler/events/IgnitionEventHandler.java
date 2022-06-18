/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.handler.events;

import java.util.Collections;
import java.util.Map;

import io.netty.channel.ChannelHandler;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import javax.inject.Inject;

@ChannelHandler.Sharable
public class IgnitionEventHandler extends BaseEventHandler {

    private final CacheManager cacheManager;

    @Inject
    public IgnitionEventHandler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {
        Device device = cacheManager.getObject(Device.class, position.getDeviceId());
        if (device == null || !PositionUtil.isLatest(cacheManager, position)) {
            return null;
        }

        Map<Event, Position> result = null;

        if (position.getAttributes().containsKey(Position.KEY_IGNITION)) {
            boolean ignition = position.getBoolean(Position.KEY_IGNITION);

            Position lastPosition = cacheManager.getPosition(position.getDeviceId());
            if (lastPosition != null && lastPosition.getAttributes().containsKey(Position.KEY_IGNITION)) {
                boolean oldIgnition = lastPosition.getBoolean(Position.KEY_IGNITION);

                if (ignition && !oldIgnition) {
                    result = Collections.singletonMap(
                            new Event(Event.TYPE_IGNITION_ON, position), position);
                } else if (!ignition && oldIgnition) {
                    result = Collections.singletonMap(
                            new Event(Event.TYPE_IGNITION_OFF, position), position);
                }
            }
        }
        return result;
    }

}
