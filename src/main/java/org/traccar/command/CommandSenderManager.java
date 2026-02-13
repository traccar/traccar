/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.command;

import com.google.inject.Injector;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Device;

import java.util.Map;

@Singleton
public class CommandSenderManager {

    private static final Map<String, Class<? extends CommandSender>> SENDERS_ALL = Map.of(
            "firebase", FirebaseCommandSender.class,
            "traccar", TraccarCommandSender.class,
            "findHub", FindHubCommandSender.class);

    private final Config config;
    private final Injector injector;

    @Inject
    public CommandSenderManager(Config config, Injector injector) {
        this.config = config;
        this.injector = injector;
    }

    public CommandSender getSender(Device device) {
        String senderType = device.getString(Keys.COMMAND_SENDER.getKey());
        if (senderType != null) {
            return injector.getInstance(SENDERS_ALL.get(senderType));
        } else if (device.hasAttribute("notificationTokens")) {
            if (config.hasKey(Keys.COMMAND_CLIENT_SERVICE_ACCOUNT)) {
                return injector.getInstance(FirebaseCommandSender.class);
            } else if (config.hasKey(Keys.NOTIFICATOR_TRACCAR_KEY)) {
                return injector.getInstance(TraccarCommandSender.class);
            }
        }
        return null;
    }

}
