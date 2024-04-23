/*
 * Copyright 2012 - 2024 Anton Tananaev (anton@traccar.org)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.geocoder.Geocoder;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

public class GeocoderHandler extends BasePositionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeocoderHandler.class);

    private final Geocoder geocoder;
    private final CacheManager cacheManager;
    private final boolean ignorePositions;
    private final boolean processInvalidPositions;
    private final int reuseDistance;

    public GeocoderHandler(Config config, Geocoder geocoder, CacheManager cacheManager) {
        this.geocoder = geocoder;
        this.cacheManager = cacheManager;
        ignorePositions = config.getBoolean(Keys.GEOCODER_IGNORE_POSITIONS);
        processInvalidPositions = config.getBoolean(Keys.GEOCODER_PROCESS_INVALID_POSITIONS);
        reuseDistance = config.getInteger(Keys.GEOCODER_REUSE_DISTANCE, 0);
    }

    @Override
    public void handlePosition(Position position, Callback callback) {
        if (!ignorePositions && (processInvalidPositions || position.getValid())) {
            if (reuseDistance != 0) {
                Position lastPosition = cacheManager.getPosition(position.getDeviceId());
                if (lastPosition != null && lastPosition.getAddress() != null
                        && position.getDouble(Position.KEY_DISTANCE) <= reuseDistance) {
                    position.setAddress(lastPosition.getAddress());
                    callback.processed(false);
                    return;
                }
            }

            geocoder.getAddress(position.getLatitude(), position.getLongitude(),
                    new Geocoder.ReverseGeocoderCallback() {
                @Override
                public void onSuccess(String address) {
                    position.setAddress(address);
                    callback.processed(false);
                }

                @Override
                public void onFailure(Throwable e) {
                    LOGGER.warn("Geocoding failed", e);
                    callback.processed(false);
                }
            });
        } else {
            callback.processed(false);
        }
    }

}
