/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
 * Copyright 2016 Andrey Kunitsyn (abyss@fox5.ru)
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

import java.util.ArrayList;
import java.util.Collection;

import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class IgnitionEventHandler extends BaseEventHandler {

    @Override
    protected Collection<Event> analyzePosition(Position position) {
        Device device = Context.getIdentityManager().getDeviceById(position.getDeviceId());
        if (device == null) {
            return null;
        }
        if (!Context.getIdentityManager().isLatestPosition(position) || !position.getValid()) {
            return null;
        }

        Collection<Event> result = null;

        boolean ignition = false;
        Object ignitionObject = position.getAttributes().get(Position.KEY_IGNITION);
        if (ignitionObject != null && ignitionObject instanceof Boolean) {
            ignition = (Boolean) ignitionObject;
        }

        boolean oldIgnition = false;
        Object oldIgnitionObject = null;
        Position lastPosition = Context.getIdentityManager().getLastPosition(position.getDeviceId());
        if (lastPosition != null) {
            oldIgnitionObject = lastPosition.getAttributes().get(Position.KEY_IGNITION);
        }
        if (oldIgnitionObject != null && oldIgnitionObject instanceof Boolean) {
            oldIgnition = (Boolean) oldIgnitionObject;
        }

        if (ignition && !oldIgnition) {
            result = new ArrayList<>();
            result.add(new Event(Event.TYPE_IGNITION_ON, position.getDeviceId(), position.getId()));
        } else if (!ignition && oldIgnition) {
            result = new ArrayList<>();
            result.add(new Event(Event.TYPE_IGNITION_OFF, position.getDeviceId(), position.getId()));
        }
        return result;
    }

}
