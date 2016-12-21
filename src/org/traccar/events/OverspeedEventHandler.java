/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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

import java.util.Collection;
import java.util.Collections;

import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class OverspeedEventHandler extends BaseEventHandler {

    public static final String ATTRIBUTE_SPEED_LIMIT = "speedLimit";

    private boolean notRepeat;

    public OverspeedEventHandler() {
        notRepeat = Context.getConfig().getBoolean("event.overspeed.notRepeat");
    }

    @Override
    protected Collection<Event> analyzePosition(Position position) {

        Device device = Context.getIdentityManager().getDeviceById(position.getDeviceId());
        if (device == null) {
            return null;
        }
        if (!Context.getIdentityManager().isLatestPosition(position) || !position.getValid()) {
            return null;
        }

        double speed = position.getSpeed();
        double speedLimit = Context.getDeviceManager()
                .lookupAttributeDouble(device.getId(), ATTRIBUTE_SPEED_LIMIT, 0, false);
        if (speedLimit == 0) {
            return null;
        }
        double oldSpeed = 0;
        if (notRepeat) {
            Position lastPosition = Context.getIdentityManager().getLastPosition(position.getDeviceId());
            if (lastPosition != null) {
                oldSpeed = lastPosition.getSpeed();
            }
        }
        if (speed > speedLimit && oldSpeed <= speedLimit) {
            Event event = new Event(Event.TYPE_DEVICE_OVERSPEED, position.getDeviceId(), position.getId());
            event.set("speed", speed);
            event.set(ATTRIBUTE_SPEED_LIMIT, speedLimit);
            return Collections.singleton(event);
        }
        return null;
    }

}
