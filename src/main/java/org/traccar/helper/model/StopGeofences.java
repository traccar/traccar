/*
 * Copyright 2026 Timothy Anderson
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

import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.reports.model.StopReportItem;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class StopGeofences {

    private final Map<Long, String> names;

    private StopGeofences(Map<Long, String> names) {
        this.names = names;
    }

    public static StopGeofences forUser(Storage storage, long userId) throws StorageException {
        var geofences = storage.getObjects(Geofence.class, new Request(
                new Columns.Include("id", "name"), new Condition.Permission(User.class, userId, Geofence.class)));
        return new StopGeofences(geofences.stream().collect(Collectors.toMap(Geofence::getId, Geofence::getName)));
    }

    private List<Long> visibleIds(List<Long> recordedIds) {
        return recordedIds == null ? List.of()
                : recordedIds.stream().filter(names::containsKey).distinct().sorted().toList();
    }

    private String displayNames(List<Long> ids) {
        return ids.stream().map(names::get).collect(Collectors.joining(", "));
    }

    public void applyToStop(StopReportItem stop) {
        var ids = visibleIds(stop.getGeofenceIds());
        stop.setGeofenceIds(ids);
        stop.setGeofenceNames(displayNames(ids));
    }

    public Event forEvent(Event event, Position position) {
        if (!Event.TYPE_DEVICE_STOPPED.equals(event.getType()) || position == null
                || position.getId() != event.getPositionId() || position.getDeviceId() != event.getDeviceId()) {
            return event;
        }
        var ids = visibleIds(position.getGeofenceIds());
        if (ids.isEmpty()) {
            return event;
        }
        Event result = new Event(event);
        if (ids.size() == 1) {
            result.setGeofenceId(ids.getFirst());
        }
        result.getAttributes().put("geofenceIds", ids);
        result.set("geofenceNames", displayNames(ids));
        return result;
    }

}
