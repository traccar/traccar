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
package org.traccar.notification;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.reports.ReportUtils;

public final class NotificationFormatter {

    private NotificationFormatter() {
    }

    public static MailMessage formatMessage(long userId, Event event, Position position) {
        Device device = Context.getIdentityManager().getDeviceById(event.getDeviceId());

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("device", device);
        velocityContext.put("event", event);
        if (position != null) {
            velocityContext.put("position", position);
            velocityContext.put("speedUnits", ReportUtils.getSpeedUnit(userId));
        }
        if (event.getGeofenceId() != 0) {
            velocityContext.put("geofence", Context.getGeofenceManager().getGeofence(event.getGeofenceId()));
        }
        velocityContext.put("webUrl", Context.getVelocityEngine().getProperty("web.url"));

        Template template = null;
        try {
            template = Context.getVelocityEngine().getTemplate(event.getType() + ".vm", StandardCharsets.UTF_8.name());
        } catch (ResourceNotFoundException error) {
            Log.warning(error);
            template = Context.getVelocityEngine().getTemplate("unknown.vm", StandardCharsets.UTF_8.name());
        }

        StringWriter writer = new StringWriter();
        template.merge(velocityContext, writer);
        String subject = (String) velocityContext.get("subject");
        return new MailMessage(subject, writer.toString());
    }
}
