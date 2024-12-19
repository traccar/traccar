/*
 * Copyright 2017 - 2024 Anton Tananaev (anton@traccar.org)
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

import jakarta.inject.Inject;
import org.traccar.config.Keys;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

public class FuelEventHandler extends BaseEventHandler {

    private final CacheManager cacheManager;

    @Inject
    public FuelEventHandler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void onPosition(Position position, Callback callback) {

        Device device = cacheManager.getObject(Device.class, position.getDeviceId());
        if (device == null) {
            return;
        }
        if (!PositionUtil.isLatest(cacheManager, position)) {
            return;
        }

        if (position.hasAttribute(Position.KEY_FUEL_LEVEL)) {
            Position lastPosition = cacheManager.getPosition(position.getDeviceId());
            if (lastPosition != null && lastPosition.hasAttribute(Position.KEY_FUEL_LEVEL)) {
                double before = lastPosition.getDouble(Position.KEY_FUEL_LEVEL);
                double after = position.getDouble(Position.KEY_FUEL_LEVEL);
                double change = after - before;

                if (change > 0) {
                    double threshold = AttributeUtil.lookup(
                            cacheManager, Keys.EVENT_FUEL_INCREASE_THRESHOLD, position.getDeviceId());
                    if (threshold > 0 && change >= threshold) {
                        callback.eventDetected(new Event(Event.TYPE_DEVICE_FUEL_INCREASE, position));
                    }
                } else if (change < 0) {
                    double threshold = AttributeUtil.lookup(
                            cacheManager, Keys.EVENT_FUEL_DROP_THRESHOLD, position.getDeviceId());
                    if (threshold > 0 && Math.abs(change) >= threshold) {
                        callback.eventDetected(new Event(Event.TYPE_DEVICE_FUEL_DROP, position));
                    }
                }
            }
        }
    }

}
