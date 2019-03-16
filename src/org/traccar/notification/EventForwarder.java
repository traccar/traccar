/*
 * Copyright 2016 - 2019 Anton Tananaev (anton@traccar.org)
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
package org.traccar.notification;

import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Maintenance;
import org.traccar.model.Position;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Invocation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class EventForwarder {

    private final String url;
    private final String header;

    public EventForwarder() {
        url = Context.getConfig().getString("event.forward.url", "http://localhost/");
        header = Context.getConfig().getString("event.forward.header");
    }

    private static final String KEY_POSITION = "position";
    private static final String KEY_EVENT = "event";
    private static final String KEY_GEOFENCE = "geofence";
    private static final String KEY_DEVICE = "device";
    private static final String KEY_MAINTENANCE = "maintenance";
    private static final String KEY_USERS = "users";

    public final void forwardEvent(Event event, Position position, Set<Long> users) {

        Invocation.Builder requestBuilder = Context.getClient().target(url).request();

        if (header != null && !header.isEmpty()) {
            for (String line: header.split("\\r?\\n")) {
                String[] values = line.split(":", 2);
                requestBuilder.header(values[0].trim(), values[1].trim());
            }
        }

        executeRequest(event, position, users, requestBuilder.async());
    }

    protected Map<String, Object> preparePayload(Event event, Position position, Set<Long> users) {
        Map<String, Object> data = new HashMap<>();
        data.put(KEY_EVENT, event);
        if (position != null) {
            data.put(KEY_POSITION, position);
        }
        Device device = Context.getIdentityManager().getById(event.getDeviceId());
        if (device != null) {
            data.put(KEY_DEVICE, device);
        }
        if (event.getGeofenceId() != 0) {
            Geofence geofence = Context.getGeofenceManager().getById(event.getGeofenceId());
            if (geofence != null) {
                data.put(KEY_GEOFENCE, geofence);
            }
        }
        if (event.getMaintenanceId() != 0) {
            Maintenance maintenance = Context.getMaintenancesManager().getById(event.getMaintenanceId());
            if (maintenance != null) {
                data.put(KEY_MAINTENANCE, maintenance);
            }
        }
        data.put(KEY_USERS, Context.getUsersManager().getItems(users));
        return data;
    }

    protected abstract void executeRequest(
            Event event, Position position, Set<Long> users, AsyncInvoker invoker);

}
