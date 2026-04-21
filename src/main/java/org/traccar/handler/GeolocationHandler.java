/*
 * Copyright 2015 - 2024 Anton Tananaev (anton@traccar.org)
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
import org.traccar.database.StatisticsManager;
import org.traccar.geolocation.GeolocationProvider;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

public class GeolocationHandler extends BasePositionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeolocationHandler.class);

    private final GeolocationProvider geolocationProvider;
    private final CacheManager cacheManager;
    private final StatisticsManager statisticsManager;
    private final boolean processInvalidPositions;
    private final boolean reuse;
    private final boolean requireWifi;

    public GeolocationHandler(
            Config config, GeolocationProvider geolocationProvider, CacheManager cacheManager,
            StatisticsManager statisticsManager) {
        this.geolocationProvider = geolocationProvider;
        this.cacheManager = cacheManager;
        this.statisticsManager = statisticsManager;
        processInvalidPositions = config.getBoolean(Keys.GEOLOCATION_PROCESS_INVALID_POSITIONS);
        reuse = config.getBoolean(Keys.GEOLOCATION_REUSE);
        requireWifi = config.getBoolean(Keys.GEOLOCATION_REQUIRE_WIFI);
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        if ((position.getOutdated() || processInvalidPositions && !position.getValid())
                && position.getNetwork() != null
                && (!requireWifi || position.getNetwork().getWifiAccessPoints() != null)) {
            if (reuse) {
                Position lastPosition = cacheManager.getPosition(position.getDeviceId());
                if (lastPosition != null && position.getNetwork().equals(lastPosition.getNetwork())) {
                    updatePosition(
                            position, lastPosition.getLatitude(), lastPosition.getLongitude(),
                            lastPosition.getAccuracy());
                    callback.processed(false);
                    return;
                }
            }

            if (statisticsManager != null) {
                statisticsManager.registerGeolocationRequest();
            }

            geolocationProvider.getLocation(position.getNetwork(),
                    new GeolocationProvider.LocationProviderCallback() {
                @Override
                public void onSuccess(double latitude, double longitude, double accuracy) {
                    updatePosition(position, latitude, longitude, accuracy);
                    callback.processed(false);
                }

                @Override
                public void onFailure(Throwable e) {
                    LOGGER.warn("Geolocation network error", e);
                    callback.processed(false);
                }
            });
        } else {
            callback.processed(false);
        }
    }

    private void updatePosition(Position position, double latitude, double longitude, double accuracy) {
        position.set(Position.KEY_APPROXIMATE, true);
        position.setValid(true);
        position.setFixTime(position.getDeviceTime());
        position.setLatitude(latitude);
        position.setLongitude(longitude);
        position.setAccuracy(accuracy);
        position.setAltitude(0);
        position.setSpeed(0);
        position.setCourse(0);
    }

}
