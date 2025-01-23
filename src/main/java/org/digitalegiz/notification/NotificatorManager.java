/*
 * Copyright 2018 - 2023 Anton Tananaev (anton@digitalegiz.org)
 * Copyright 2018 Andrey Kunitsyn (andrey@digitalegiz.org)
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
package org.digitalegiz.notification;

import com.google.inject.Injector;
import org.digitalegiz.config.Config;
import org.digitalegiz.config.Keys;
import org.digitalegiz.model.Typed;
import org.digitalegiz.notificators.Notificator;
import org.digitalegiz.notificators.NotificatorCommand;
import org.digitalegiz.notificators.NotificatorFirebase;
import org.digitalegiz.notificators.NotificatorMail;
import org.digitalegiz.notificators.NotificatorPushover;
import org.digitalegiz.notificators.NotificatorSms;
import org.digitalegiz.notificators.NotificatorTelegram;
import org.digitalegiz.notificators.Notificatordigitalegiz;
import org.digitalegiz.notificators.NotificatorWeb;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class NotificatorManager {

    private static final Map<String, Class<? extends Notificator>> NOTIFICATORS_ALL = Map.of(
            "command", NotificatorCommand.class,
            "web", NotificatorWeb.class,
            "mail", NotificatorMail.class,
            "sms", NotificatorSms.class,
            "firebase", NotificatorFirebase.class,
            "digitalegiz", Notificatordigitalegiz.class,
            "telegram", NotificatorTelegram.class,
            "pushover", NotificatorPushover.class);

    private final Injector injector;

    private final Set<String> types = new HashSet<>();

    @Inject
    public NotificatorManager(Injector injector, Config config) {
        this.injector = injector;
        String types = config.getString(Keys.NOTIFICATOR_TYPES);
        if (types != null) {
            this.types.addAll(Arrays.asList(types.split(",")));
        }
    }

    public Notificator getNotificator(String type) {
        var clazz = NOTIFICATORS_ALL.get(type);
        if (clazz != null && types.contains(type)) {
            var notificator = injector.getInstance(clazz);
            if (notificator != null) {
                return notificator;
            }
        }
        throw new RuntimeException("Failed to get notificator " + type);
    }

    public Set<Typed> getAllNotificatorTypes() {
        return types.stream().map(Typed::new).collect(Collectors.toUnmodifiableSet());
    }

}
