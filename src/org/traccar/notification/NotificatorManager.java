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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.traccar.Context;
import org.traccar.helper.Log;

public final class NotificatorManager {

    public NotificatorManager() {
        final String[] types = Context.getConfig().getString("notificator.types", "").split(",");
        for (String type : types) {
            final String className = Context.getConfig().getString("notificator." + type + ".class", "");
            if (className.length() > 0) {
                try {
                    final Class<Notificator> c = (Class<Notificator>) Class.forName(className);
                    try {
                        final Constructor<Notificator> constructor = c.getConstructor(new Class[]{String.class});
                        notificators.put(type, constructor.newInstance(type));
                    } catch (NoSuchMethodException e) {
                        // No constructor with String argument
                        notificators.put(type, c.newInstance());
                    }
                } catch (ClassNotFoundException | InstantiationException
                        | IllegalAccessException | InvocationTargetException e) {
                    Log.error("Unable to load notificator class for " + type + " " + className + " " + e.getMessage());
                }
            }
        }
    }

    private final Map<String, Notificator> notificators = new HashMap<>();
    private static final Notificator NULL_NOTIFICATOR = new NotificationNull();

    public Notificator getNotificator(String type) {
        final Notificator n = notificators.get(type);
        if (n == null) {
            Log.error("No notificator configured for type : " + type);
            return NULL_NOTIFICATOR;
        }
        return n;
    }


    public Set<String> getNotificatorTypes() {
        return notificators.keySet();
    }


}

