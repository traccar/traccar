/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
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
package org.traccar.schedule;

import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.mail.MailManager;
import org.traccar.model.Device;
import org.traccar.model.Disableable;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.notification.TextTemplateFormatter;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskExpirations implements ScheduleTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExpirations.class);

    private static final long CHECK_PERIOD_HOURS = 1;

    private final Config config;
    private final Storage storage;
    private final TextTemplateFormatter textTemplateFormatter;
    private final MailManager mailManager;

    @Inject
    public TaskExpirations(
            Config config, Storage storage, TextTemplateFormatter textTemplateFormatter, MailManager mailManager) {
        this.config = config;
        this.storage = storage;
        this.textTemplateFormatter = textTemplateFormatter;
        this.mailManager = mailManager;
    }

    @Override
    public void schedule(ScheduledExecutorService executor) {
        executor.scheduleAtFixedRate(this, CHECK_PERIOD_HOURS, CHECK_PERIOD_HOURS, TimeUnit.HOURS);
    }

    private boolean checkTimeTrigger(Disableable disableable, long currentTime, long offsetTime) {
        if (disableable.getExpirationTime() != null) {
            long previousTime = currentTime - TimeUnit.HOURS.toMillis(CHECK_PERIOD_HOURS);
            long expirationTime = disableable.getExpirationTime().getTime() + offsetTime;
            return previousTime < expirationTime && currentTime >= expirationTime;
        }
        return false;
    }

    private void sendUserExpiration(
            Server server, User user, String template) throws MessagingException {
        var velocityContext = textTemplateFormatter.prepareContext(server, user);
        velocityContext.put("expiration", user.getExpirationTime());
        var fullMessage = textTemplateFormatter.formatMessage(velocityContext, template, "full");
        mailManager.sendMessage(user, true, fullMessage.getSubject(), fullMessage.getBody());
    }

    private void sendDeviceExpiration(
            Server server, Device device, String template) throws MessagingException, StorageException {
        var users = storage.getObjects(User.class, new Request(
                new Columns.All(), new Condition.Permission(User.class, Device.class, device.getId())));
        for (User user : users) {
            var velocityContext = textTemplateFormatter.prepareContext(server, user);
            velocityContext.put("expiration", device.getExpirationTime());
            velocityContext.put("device", device);
            var fullMessage = textTemplateFormatter.formatMessage(velocityContext, template, "full");
            mailManager.sendMessage(user, true, fullMessage.getSubject(), fullMessage.getBody());
        }
    }

    @Override
    public void run() {
        try {

            long currentTime = System.currentTimeMillis();
            Server server = storage.getObject(Server.class, new Request(new Columns.All()));

            if (config.getBoolean(Keys.NOTIFICATION_EXPIRATION_USER)) {
                long reminder = config.getLong(Keys.NOTIFICATION_EXPIRATION_USER_REMINDER);
                var users = storage.getObjects(User.class, new Request(new Columns.All()));
                for (User user : users) {
                    if (checkTimeTrigger(user, currentTime, 0)) {
                        sendUserExpiration(server, user, "userExpiration");
                    } else if (reminder > 0 && checkTimeTrigger(user, currentTime, -reminder)) {
                        sendUserExpiration(server, user, "userExpirationReminder");
                    }
                }
            }

            if (config.getBoolean(Keys.NOTIFICATION_EXPIRATION_DEVICE)) {
                long reminder = config.getLong(Keys.NOTIFICATION_EXPIRATION_USER_REMINDER);
                var devices = storage.getObjects(Device.class, new Request(new Columns.All()));
                for (Device device : devices) {
                    if (checkTimeTrigger(device, currentTime, 0)) {
                        sendDeviceExpiration(server, device, "deviceExpiration");
                    } else if (reminder > 0 && checkTimeTrigger(device, currentTime, -reminder)) {
                        sendDeviceExpiration(server, device, "deviceExpirationReminder");
                    }
                }
            }

        } catch (StorageException | MessagingException e) {
            LOGGER.warn("Failed to check expirations", e);
        }
    }

}
