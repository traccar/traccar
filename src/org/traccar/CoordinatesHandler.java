/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar;

import org.traccar.helper.DistanceCalculator;
import org.traccar.model.Position;

public class CoordinatesHandler extends BaseDataHandler {

    private final int coordinatesError;

    public CoordinatesHandler() {
        Config config = Context.getConfig();
        coordinatesError = config.getInteger("coordinates.error", 50);
    }

    private Position getLastPosition(long deviceId) {
        if (Context.getIdentityManager() != null) {
            return Context.getIdentityManager().getLastPosition(deviceId);
        }
        return null;
    }

    @Override
    protected Position handlePosition(Position position) {
        Position last = getLastPosition(position.getDeviceId());
        if (last != null) {
            double distance = DistanceCalculator.distance(
                    position.getLatitude(), position.getLongitude(), last.getLatitude(), last.getLongitude());
            if (distance < coordinatesError) {
                position.setLatitude(last.getLatitude());
                position.setLongitude(last.getLongitude());
            }
        }
        return position;
    }

}
