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
package org.traccar.handler.events;

import jakarta.inject.Inject;
import org.traccar.config.Keys;
import org.traccar.helper.DistanceCalculator;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.util.Set;

public class ProximityEventHandler extends BaseEventHandler {

    private final CacheManager cacheManager;

    @Inject
    public ProximityEventHandler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        if (!PositionUtil.isLatest(cacheManager, position)) {
            return;
        }

        long deviceId = position.getDeviceId();
        Set<Device> linkedDevices = cacheManager.getDeviceObjects(deviceId, Device.class);
        if (linkedDevices.isEmpty()) {
            return;
        }

        var attributeProvider = new AttributeUtil.CacheProvider(cacheManager, deviceId);
        double enterDistance = AttributeUtil.lookup(attributeProvider, Keys.EVENT_PROXIMITY_ENTER_DISTANCE);
        double exitDistance = AttributeUtil.lookup(attributeProvider, Keys.EVENT_PROXIMITY_EXIT_DISTANCE);
        double unaccompaniedDistance = AttributeUtil.lookup(attributeProvider, Keys.EVENT_UNACCOMPANIED_DISTANCE);

        Position lastPosition = cacheManager.getPosition(deviceId);
        if (lastPosition == null) {
            return;
        }

        boolean checkUnaccompanied = unaccompaniedDistance > 0
                && !lastPosition.getBoolean(Position.KEY_MOTION)
                && position.getBoolean(Position.KEY_MOTION);
        if (enterDistance <= 0 && exitDistance <= 0 && !checkUnaccompanied) {
            return;
        }

        double maxDistance = Math.max(Math.max(enterDistance, exitDistance), unaccompaniedDistance);
        double latitudeDelta = DistanceCalculator.getLatitudeDelta(maxDistance);
        double longitudeDelta = DistanceCalculator.getLongitudeDelta(maxDistance, position.getLatitude());
        boolean anyAccompanied = false;

        for (Device linkedDevice : linkedDevices) {
            Position linkedPosition = cacheManager.getPosition(linkedDevice.getId());
            if (linkedPosition == null) {
                continue;
            }

            double distanceNew = boundedDistance(position, linkedPosition, latitudeDelta, longitudeDelta);
            double distanceOld = boundedDistance(lastPosition, linkedPosition, latitudeDelta, longitudeDelta);

            if (enterDistance > 0 && distanceOld > enterDistance && distanceNew <= enterDistance) {
                Event event = new Event(Event.TYPE_PROXIMITY_ENTER, position);
                event.set("linkedDeviceId", linkedDevice.getId());
                callback.eventDetected(event);
            } else if (exitDistance > 0 && distanceOld <= exitDistance && distanceNew > exitDistance) {
                Event event = new Event(Event.TYPE_PROXIMITY_EXIT, position);
                event.set("linkedDeviceId", linkedDevice.getId());
                callback.eventDetected(event);
            }

            if (distanceNew <= unaccompaniedDistance) {
                anyAccompanied = true;
            }
        }

        if (checkUnaccompanied && !anyAccompanied) {
            callback.eventDetected(new Event(Event.TYPE_UNACCOMPANIED_MOTION, position));
        }
    }

    private static double boundedDistance(
            Position from, Position to, double latitudeDelta, double longitudeDelta) {
        if (Math.abs(from.getLatitude() - to.getLatitude()) > latitudeDelta
                || Math.abs(from.getLongitude() - to.getLongitude()) > longitudeDelta) {
            return Double.POSITIVE_INFINITY;
        }
        return DistanceCalculator.distance(from, to);
    }
}
