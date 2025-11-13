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

import java.util.Map;

@Singleton
public class CommandSenderManager {

    private static final Map<String, Class<? extends CommandSender>> SENDERS_ALL = Map.of(
            "client", ClientCommandSender.class,
            "findHub", FindHubCommandSender.class);

    private final Injector injector;

    @Inject
    public CommandSenderManager(Injector injector) {
        this.injector = injector;
    }

    public CommandSender getSender(String type) {
        var clazz = SENDERS_ALL.get(type);
        var sender = injector.getInstance(clazz);
        if (sender != null) {
            return sender;
        }
        throw new RuntimeException("Failed to get command sender " + type);
    }

}
