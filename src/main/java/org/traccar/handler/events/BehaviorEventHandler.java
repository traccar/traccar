/*
 * Copyright 2021 - 2024 Anton Tananaev (anton@traccar.org)
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
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

public class BehaviorEventHandler extends BaseEventHandler {

    private final double accelerationThreshold;
    private final double brakingThreshold;

    private final CacheManager cacheManager;

    @Inject
    public BehaviorEventHandler(Config config, CacheManager cacheManager) {
        accelerationThreshold = config.getDouble(Keys.EVENT_BEHAVIOR_ACCELERATION_THRESHOLD);
        brakingThreshold = config.getDouble(Keys.EVENT_BEHAVIOR_BRAKING_THRESHOLD);
        this.cacheManager = cacheManager;
    }

    @Override
    public void analyzePosition(Position position, Callback callback) {

        Position lastPosition = cacheManager.getPosition(position.getDeviceId());
        if (lastPosition != null && position.getFixTime().equals(lastPosition.getFixTime())) {
            double acceleration = UnitsConverter.mpsFromKnots(position.getSpeed() - lastPosition.getSpeed()) * 1000
                    / (position.getFixTime().getTime() - lastPosition.getFixTime().getTime());
            if (accelerationThreshold != 0 && acceleration >= accelerationThreshold) {
                Event event = new Event(Event.TYPE_ALARM, position);
                event.set(Position.KEY_ALARM, Position.ALARM_ACCELERATION);
                callback.eventDetected(event);
            } else if (brakingThreshold != 0 && acceleration <= -brakingThreshold) {
                Event event = new Event(Event.TYPE_ALARM, position);
                event.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
                callback.eventDetected(event);
            }
        }
    }

}
