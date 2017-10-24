/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.reports.ReportUtils;

public final class NotificationFormatter {

    private NotificationFormatter() {
    }

    public static VelocityContext prepareContext(long userId, Event event, Position position) {
        User user = Context.getPermissionsManager().getUser(userId);
        Device device = Context.getIdentityManager().getById(event.getDeviceId());

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("user", user);
        velocityContext.put("device", device);
        velocityContext.put("event", event);
        if (position != null) {
            velocityContext.put("position", position);
            velocityContext.put("speedUnit", ReportUtils.getSpeedUnit(userId));
            velocityContext.put("distanceUnit", ReportUtils.getDistanceUnit(userId));
            velocityContext.put("volumeUnit", ReportUtils.getVolumeUnit(userId));
        }
        if (event.getGeofenceId() != 0) {
            velocityContext.put("geofence", Context.getGeofenceManager().getById(event.getGeofenceId()));
        }
        String driverUniqueId = event.getString(Position.KEY_DRIVER_UNIQUE_ID);
        if (driverUniqueId != null) {
            velocityContext.put("driver", Context.getDriversManager().getDriverByUniqueId(driverUniqueId));
        }
        velocityContext.put("webUrl", Context.getVelocityEngine().getProperty("web.url"));
        velocityContext.put("dateTool", new DateTool());
        velocityContext.put("numberTool", new NumberTool());
        velocityContext.put("timezone", ReportUtils.getTimezone(userId));
        velocityContext.put("locale", Locale.getDefault());
        return velocityContext;
    }

    public static Template getTemplate(Event event, String path) {
        Template template;
        try {
            template = Context.getVelocityEngine().getTemplate(path + event.getType() + ".vm",
                    StandardCharsets.UTF_8.name());
        } catch (ResourceNotFoundException error) {
            Log.warning(error);
            template = Context.getVelocityEngine().getTemplate(path + "unknown.vm",
                    StandardCharsets.UTF_8.name());
        }
        return template;
    }

    public static MailMessage formatMailMessage(long userId, Event event, Position position) {
        VelocityContext velocityContext = prepareContext(userId, event, position);
        StringWriter writer = new StringWriter();
        getTemplate(event, Context.getConfig().getString("mail.templatesPath", "mail") + "/")
                .merge(velocityContext, writer);
        return new MailMessage((String) velocityContext.get("subject"), writer.toString());
    }

    public static String formatSmsMessage(long userId, Event event, Position position) {
        VelocityContext velocityContext = prepareContext(userId, event, position);
        StringWriter writer = new StringWriter();
        getTemplate(event, Context.getConfig().getString("sms.templatesPath", "sms") + "/")
                .merge(velocityContext, writer);
        return writer.toString();
    }
}
