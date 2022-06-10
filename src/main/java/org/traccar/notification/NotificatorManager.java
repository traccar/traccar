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
package org.traccar.notification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Main;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Typed;
import org.traccar.notificators.NotificatorFirebase;
import org.traccar.notificators.NotificatorMail;
import org.traccar.notificators.NotificatorNull;
import org.traccar.notificators.Notificator;
import org.traccar.notificators.NotificatorSms;
import org.traccar.notificators.NotificatorTraccar;
import org.traccar.notificators.NotificatorWeb;
import org.traccar.notificators.NotificatorTelegram;
import org.traccar.notificators.NotificatorPushover;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NotificatorManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorManager.class);

    private static final Map<String, Class<? extends Notificator>> NOTIFICATORS_ALL = Map.of(
            "web", NotificatorWeb.class,
            "mail", NotificatorMail.class,
            "sms", NotificatorSms.class,
            "firebase", NotificatorFirebase.class,
            "traccar", NotificatorTraccar.class,
            "telegram", NotificatorTelegram.class,
            "pushover", NotificatorPushover.class);

    private final Map<String, Notificator> notificators = new HashMap<>();

    @Inject
    public NotificatorManager(Config config) {
        String types = config.getString(Keys.NOTIFICATOR_TYPES);
        if (types != null) {
            for (String type : types.split(",")) {
                var notificatorClass = NOTIFICATORS_ALL.get(type);
                if (notificatorClass != null) {
                    notificators.put(type, Main.getInjector().getInstance(notificatorClass));
                }
            }
        }
    }

    public Notificator getNotificator(String type) {
        final Notificator notificator = notificators.get(type);
        if (notificator == null) {
            LOGGER.warn("No notificator configured for type : " + type);
            return new NotificatorNull();
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
