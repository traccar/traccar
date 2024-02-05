/*
 * Copyright 2016 - 2023 Anton Tananaev (anton@traccar.org)
 * Copyright 2015 Amila Silva
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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.traccar.BaseDataHandler;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.DistanceCalculator;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

@Singleton
@ChannelHandler.Sharable
public class DistanceHandler extends BaseDataHandler {

    private final CacheManager cacheManager;

    private final boolean filter;
    private final int minError;
    private final int maxError;

    @Inject
    public DistanceHandler(Config config, CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.filter = config.getBoolean(Keys.COORDINATES_FILTER);
        this.minError = config.getInteger(Keys.COORDINATES_MIN_ERROR);
        this.maxError = config.getInteger(Keys.COORDINATES_MAX_ERROR);
    }

    @Override
    protected Position handlePosition(Position position) {

        double distance = 0.0;
        if (position.hasAttribute(Position.KEY_DISTANCE)) {
            distance = position.getDouble(Position.KEY_DISTANCE);
        }
        double totalDistance;
        Position last = cacheManager.getPosition(position.getDeviceId());
        if (last != null) {
            totalDistance = last.getDouble(Position.KEY_TOTAL_DISTANCE);
            if (!position.hasAttribute(Position.KEY_DISTANCE)) {
                distance = DistanceCalculator.distance(
                        position.getLatitude(), position.getLongitude(),
                        last.getLatitude(), last.getLongitude());
            }
            if (filter && last.getLatitude() != 0 && last.getLongitude() != 0) {
                boolean satisfiesMin = minError == 0 || distance > minError;
                boolean satisfiesMax = maxError == 0 || distance < maxError || position.getValid();
                if (!satisfiesMin || !satisfiesMax) {
                    position.setValid(last.getValid());
                    position.setLatitude(last.getLatitude());
                    position.setLongitude(last.getLongitude());
                    distance = 0;
                }
            }
        } else {
            totalDistance = 0.0;
        }
        position.set(Position.KEY_DISTANCE, distance);
        position.set(Position.KEY_TOTAL_DISTANCE, totalDistance + distance);

        return position;
    }

}
