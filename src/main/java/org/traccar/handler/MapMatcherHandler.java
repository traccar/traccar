/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.mapmatcher.MapMatcher;
import org.traccar.model.Position;

public class MapMatcherHandler extends BasePositionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapMatcherHandler.class);

    private final MapMatcher mapMatcher;

    public MapMatcherHandler(MapMatcher mapMatcher) {
        this.mapMatcher = mapMatcher;
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        mapMatcher.getPoint(position.getLatitude(), position.getLongitude(),
                new MapMatcher.MapMatcherCallback() {
            @Override
            public void onSuccess(double latitude, double longitude) {
                position.setLatitude(latitude);
                position.setLongitude(longitude);
                callback.processed(false);
            }

            @Override
            public void onFailure(Throwable e) {
                LOGGER.warn("Map matcher failed", e);
                callback.processed(false);
            }
        });
    }

}
