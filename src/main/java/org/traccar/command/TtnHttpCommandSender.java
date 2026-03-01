/*
 * Copyright 2026 Stephen Horvath (me@stevetech.au)
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.Command;
import org.traccar.model.Device;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Singleton
public class TtnHttpCommandSender implements CommandSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(TtnHttpCommandSender.class);

    private final Client client;

    @Inject
    public TtnHttpCommandSender(Client client) throws IOException {
        this.client = client;
    }

    @Override
    public Collection<String> getSupportedCommands() {
        return List.of(Command.TYPE_CUSTOM);
    }

    @Override
    public void sendCommand(Device device, Command command) throws Exception {
        if (!device.hasAttribute("X-Downlink-Apikey")) {
            throw new RuntimeException("Missing X-Downlink-Apikey header");
        }
        String key = device.getString("X-Downlink-Apikey");

        String url = device.getString("X-Downlink-Push");
        if (command.getBoolean(Command.KEY_NO_QUEUE) && device.hasAttribute("X-Downlink-Replace")) {
            url = device.getString("X-Downlink-Replace");
        }
        if (url == null) {
            throw new RuntimeException("Missing X-Downlink-Push or X-Downlink-Replace header");
        }

        String data = command.getString(Command.KEY_DATA);
        if (data == null) {
            throw new RuntimeException("Missing command data");
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode downlinks = root.putArray("downlinks");
        if (!data.isEmpty()) {
            ObjectNode downlink = downlinks.addObject();
            try {
                downlink.set("decoded_payload", mapper.readTree(data));
            } catch (IOException e) {
                ObjectNode payload = downlink.putObject("decoded_payload");
                payload.put("command", data);
            }
        }
        String message = mapper.writeValueAsString(root);

        var request = client.target(url).request().header("Authorization", "Bearer " + key);
        try {
            request.post(Entity.json(message));
        } catch (Exception e) {
            LOGGER.warn("Command push error", e);
        }
    }

}
