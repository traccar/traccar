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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Command;
import org.traccar.model.Device;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Singleton
public class TraccarCommandSender implements CommandSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraccarCommandSender.class);

    private final Client client;

    private final String url;
    private final String key;

    public static class Message {
        @JsonProperty("type")
        private String type;
        @JsonProperty("registration_ids")
        private String[] tokens;
        @JsonProperty("data")
        private Map<String, String> data;
    }

    @Inject
    public TraccarCommandSender(Config config, Client client) throws IOException {
        this.client = client;
        this.url = "https://www.traccar.org/push/";
        this.key = config.getString(Keys.NOTIFICATOR_TRACCAR_KEY);
    }

    @Override
    public Collection<String> getSupportedCommands() {
        return List.of(
                Command.TYPE_POSITION_SINGLE,
                Command.TYPE_POSITION_PERIODIC,
                Command.TYPE_POSITION_STOP,
                Command.TYPE_FACTORY_RESET);
    }

    @Override
    public void sendCommand(Device device, Command command) throws Exception {
        if (!device.hasAttribute("notificationTokens")) {
            throw new RuntimeException("Missing device notification tokens");
        }

        Message message = new Message();
        message.type = "client";
        message.tokens = device.getString("notificationTokens").split("[, ]");
        message.data = Map.of("command", command.getType());

        var request = client.target(url).request().header("Authorization", "key=" + key);
        try {
            request.post(Entity.json(message));
        } catch (Exception e) {
            LOGGER.warn("Command push error", e);
        }
    }

}
