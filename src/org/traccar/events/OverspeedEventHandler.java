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
package org.traccar.events;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.helper.Log;
import org.traccar.helper.UnitsConverter;

public class OverspeedEventHandler extends BaseEventHandler {

    private double globalSpeedLimit;
    private int suppressRepeated;

    public OverspeedEventHandler() {
        globalSpeedLimit = UnitsConverter.knotsFromKph(Context.getConfig().getInteger("event.globalSpeedLimit", 0));
        suppressRepeated = Context.getConfig().getInteger("event.suppressRepeated", 60);
    }

    @Override
    protected Collection<Event> analyzePosition(Position position) {

        Device device = Context.getDataManager().getDeviceById(position.getDeviceId());
        if (device == null) {
            return null;
        }
        if (position.getId() != device.getPositionId() || !position.getValid()) {
            return null;
        }

        Collection<Event> events = new ArrayList<>();
        double speed = position.getSpeed();
        boolean valid = position.getValid();

        if (valid && globalSpeedLimit != 0 && speed > globalSpeedLimit) {
            try {
                if (Context.getDataManager().getLastEvents(
                        position.getDeviceId(), Event.TYPE_DEVICE_OVERSPEED, suppressRepeated).isEmpty()) {
                    events.add(new Event(Event.TYPE_DEVICE_OVERSPEED, position.getDeviceId(), position.getId()));
                }
            } catch (SQLException error) {
                Log.warning(error);
            }

        }
        return events;
    }

}
