/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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
package org.traccar.events;

import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.Collection;
import java.util.Collections;

public class FuelDropEventHandler extends BaseEventHandler {

    public static final String ATTRIBUTE_FUEL_DROP_THRESHOLD = "fuelDropThreshold";

    @Override
    protected Collection<Event> analyzePosition(Position position) {

        Device device = Context.getIdentityManager().getDeviceById(position.getDeviceId());
        if (device == null) {
            return null;
        }
        if (!Context.getIdentityManager().isLatestPosition(position) || !position.getValid()) {
            return null;
        }

        double fuelDropThreshold = Context.getDeviceManager()
                .lookupAttributeDouble(device.getId(), ATTRIBUTE_FUEL_DROP_THRESHOLD, 0, false);

        Position lastPosition = Context.getIdentityManager().getLastPosition(position.getDeviceId());
        if (position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)
                && lastPosition != null && lastPosition.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {

            double drop = lastPosition.getDouble(Position.KEY_FUEL_LEVEL) - position.getDouble(Position.KEY_FUEL_LEVEL);
            if (drop >= fuelDropThreshold) {
                Event event = new Event(Event.TYPE_DEVICE_FUEL_DROP, position.getDeviceId(), position.getId());
                event.set(ATTRIBUTE_FUEL_DROP_THRESHOLD, fuelDropThreshold);
                return Collections.singleton(event);
            }
        }

        return null;
    }

}
