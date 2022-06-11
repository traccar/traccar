/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 - 2018 Andrey Kunitsyn (andrey@traccar.org)
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

import org.apache.velocity.VelocityContext;
import org.traccar.helper.model.UserUtil;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Maintenance;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.session.cache.CacheManager;

import javax.inject.Inject;

public class NotificationFormatter {

    private final CacheManager cacheManager;
    private final TextTemplateFormatter textTemplateFormatter;

    @Inject
    public NotificationFormatter(
            CacheManager cacheManager, TextTemplateFormatter textTemplateFormatter) {
        this.cacheManager = cacheManager;
        this.textTemplateFormatter = textTemplateFormatter;
    }

    public NotificationMessage formatMessage(User user, Event event, Position position, String templatePath) {

        Server server = cacheManager.getServer();
        Device device = cacheManager.getObject(Device.class, event.getDeviceId());

        VelocityContext velocityContext = textTemplateFormatter.prepareContext(server, user);

        velocityContext.put("device", device);
        velocityContext.put("event", event);
        if (position != null) {
            velocityContext.put("position", position);
            velocityContext.put("speedUnit", UserUtil.getSpeedUnit(server, user));
            velocityContext.put("distanceUnit", UserUtil.getDistanceUnit(server, user));
            velocityContext.put("volumeUnit", UserUtil.getVolumeUnit(server, user));
        }
        if (event.getGeofenceId() != 0) {
            velocityContext.put("geofence", cacheManager.getObject(Geofence.class, event.getGeofenceId()));
        }
        if (event.getMaintenanceId() != 0) {
            velocityContext.put("maintenance", cacheManager.getObject(Maintenance.class, event.getMaintenanceId()));
        }
        String driverUniqueId = event.getString(Position.KEY_DRIVER_UNIQUE_ID);
        if (driverUniqueId != null) {
            velocityContext.put("driver", cacheManager.findDriverByUniqueId(device.getId(), driverUniqueId));
        }

        return textTemplateFormatter.formatMessage(velocityContext, event.getType(), templatePath);
    }

}
