/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.helper.model;

import org.traccar.config.Config;
import org.traccar.model.Geofence;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.util.ArrayList;
import java.util.List;

public final class GeofenceUtil {

    private GeofenceUtil() {
    }

    public static List<Long> getCurrentGeofences(Config config, CacheManager cacheManager, Position position) {
        List<Long> result = new ArrayList<>();
        for (Geofence geofence : cacheManager.getDeviceObjects(position.getDeviceId(), Geofence.class)) {
            if (geofence.getGeometry().containsPoint(
                    config, geofence, position.getLatitude(), position.getLongitude())) {
                result.add(geofence.getId());
            }
        }
        return result;
    }

}
