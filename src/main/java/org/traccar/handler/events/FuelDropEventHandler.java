/*
 * Copyright 2017 - 2022 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.ChannelHandler;
import org.traccar.config.Keys;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;

@ChannelHandler.Sharable
public class FuelDropEventHandler extends BaseEventHandler {

    private final CacheManager cacheManager;

    @Inject
    public FuelDropEventHandler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {

        Device device = cacheManager.getObject(Device.class, position.getDeviceId());
        if (device == null) {
            return null;
        }
        if (!PositionUtil.isLatest(cacheManager, position)) {
            return null;
        }

        double fuelDropThreshold = AttributeUtil.lookup(
                cacheManager, Keys.EVENT_FUEL_DROP_THRESHOLD, position.getDeviceId());
        if (fuelDropThreshold > 0) {
            Position lastPosition = cacheManager.getPosition(position.getDeviceId());
            if (position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)
                    && lastPosition != null && lastPosition.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {

                double drop = lastPosition.getDouble(Position.KEY_FUEL_LEVEL)
                        - position.getDouble(Position.KEY_FUEL_LEVEL);
                if (drop >= fuelDropThreshold) {
                    Event event = new Event(Event.TYPE_DEVICE_FUEL_DROP, position);
                    return Collections.singletonMap(event, position);
                }
            }
        }

        return null;
    }

}
