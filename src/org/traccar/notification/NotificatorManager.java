/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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
package org.traccar.notification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.Typed;
import org.traccar.notificators.NotificatorNull;
import org.traccar.notificators.Notificator;

public final class NotificatorManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorManager.class);

    private static final String DEFAULT_WEB_NOTIFICATOR = "org.traccar.notificators.NotificatorWeb";
    private static final String DEFAULT_MAIL_NOTIFICATOR = "org.traccar.notificators.NotificatorMail";
    private static final String DEFAULT_SMS_NOTIFICATOR = "org.traccar.notificators.NotificatorSms";
    private static final String DEFAULT_FIREBASE_NOTIFICATOR = "org.traccar.notificators.NotificatorFirebase";

    private final Map<String, Notificator> notificators = new HashMap<>();
    private static final Notificator NULL_NOTIFICATOR = new NotificatorNull();

    public NotificatorManager() {
        final String[] types = Context.getConfig().getString("notificator.types", "").split(",");
        for (String type : types) {
            String defaultNotificator = "";
            switch (type) {
                case "web":
                    defaultNotificator = DEFAULT_WEB_NOTIFICATOR;
                    break;
                case "mail":
                    defaultNotificator = DEFAULT_MAIL_NOTIFICATOR;
                    break;
                case "sms":
                    defaultNotificator = DEFAULT_SMS_NOTIFICATOR;
                    break;
                case "firebase":
                    defaultNotificator = DEFAULT_FIREBASE_NOTIFICATOR;
                    break;
                default:
                    break;
            }
            final String className = Context.getConfig()
                    .getString("notificator." + type + ".class", defaultNotificator);
            try {
                notificators.put(type, (Notificator) Class.forName(className).newInstance());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                LOGGER.warn("Unable to load notificator class for " + type + " " + className + " " + e.getMessage());
            }
        }
    }

    public Notificator getNotificator(String type) {
        final Notificator notificator = notificators.get(type);
        if (notificator == null) {
            LOGGER.warn("No notificator configured for type : " + type);
            return NULL_NOTIFICATOR;
        }
        return notificator;
    }

    public Set<Typed> getAllNotificatorTypes() {
        Set<Typed> result = new HashSet<>();
        for (String notificator : notificators.keySet()) {
            result.add(new Typed(notificator));
        }
        return result;
    }

}
