/*
 * Copyright 2018 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2018 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.notificators;

import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.NotificationFormatter;
import org.traccar.session.ConnectionManager;
import org.traccar.session.cache.CacheManager;

import javax.inject.Inject;

public final class NotificatorWeb implements Notificator {

    private final ConnectionManager connectionManager;
    private final CacheManager cacheManager;

    @Inject
    public NotificatorWeb(ConnectionManager connectionManager, CacheManager cacheManager) {
        this.connectionManager = connectionManager;
        this.cacheManager = cacheManager;
    }

    @Override
    public void send(User user, Event event, Position position) {

        Event copy = new Event();
        copy.setId(event.getId());
        copy.setDeviceId(event.getDeviceId());
        copy.setType(event.getType());
        copy.setEventTime(event.getEventTime());
        copy.setPositionId(event.getPositionId());
        copy.setGeofenceId(event.getGeofenceId());
        copy.setMaintenanceId(event.getMaintenanceId());
        copy.getAttributes().putAll(event.getAttributes());

        var message = NotificationFormatter.formatMessage(cacheManager, user, event, position, "short");
        copy.set("message", message.getBody());

        connectionManager.updateEvent(user.getId(), copy);
    }

}
