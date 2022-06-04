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
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Maintenance;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.reports.common.ReportUtils;
import org.traccar.session.cache.CacheManager;

public final class NotificationFormatter {

    private NotificationFormatter() {
    }

    public static NotificationMessage formatMessage(
            CacheManager cacheManager, long userId, Event event, Position position, String templatePath) {

        User user = cacheManager.getObject(User.class, userId);
        Device device = cacheManager.getObject(Device.class, event.getDeviceId());

        VelocityContext velocityContext = TextTemplateFormatter.prepareContext(user);

        velocityContext.put("device", device);
        velocityContext.put("event", event);
        if (position != null) {
            velocityContext.put("position", position);
            velocityContext.put("speedUnit", ReportUtils.getSpeedUnit(userId));
            velocityContext.put("distanceUnit", ReportUtils.getDistanceUnit(userId));
            velocityContext.put("volumeUnit", ReportUtils.getVolumeUnit(userId));
        }
        if (event.getGeofenceId() != 0) {
            velocityContext.put("geofence", cacheManager.getObject(Geofence.class, event.getGeofenceId()));
        }
        if (event.getMaintenanceId() != 0) {
            velocityContext.put("maintenance", cacheManager.getObject(Maintenance.class, event.getMaintenanceId()));
        }
        String driverUniqueId = event.getString(Position.KEY_DRIVER_UNIQUE_ID);
        if (driverUniqueId != null) {
            velocityContext.put("driver", Context.getDriversManager().getDriverByUniqueId(driverUniqueId));
        }

        return TextTemplateFormatter.formatMessage(velocityContext, event.getType(), templatePath);
    }

}
