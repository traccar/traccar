/*
 * Copyright 2022 - 2023 Anton Tananaev (anton@traccar.org)
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
package org.traccar.forward;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class EventForwarderMqtt implements EventForwarder {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventForwarderMqtt.class);
    private final MqttClient client;
    private final ObjectMapper objectMapper;

    public EventForwarderMqtt(Config config, ObjectMapper objectMapper, MqttClientFactory clientFactory) {
        URI url;
        try {
            url = new URI(config.getString(Keys.EVENT_FORWARD_URL));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        int version = config.getInteger(Keys.EVENT_FORWARD_MQTT_VERSION);
        String host = url.getHost();
        int port = url.getPort();
        String username = null;
        String password = null;
        String topic = config.getString(Keys.EVENT_FORWARD_TOPIC);
        String userInfo = url.getUserInfo();
        if(userInfo != null) {
            int delimiter = userInfo.indexOf(':');
            if (delimiter == -1) {
                throw new IllegalArgumentException("Wrong credentials. Should be in format \"username:password\"");
            } else {
                username = userInfo.substring(0, delimiter++);
                password = userInfo.substring(delimiter);
            }
        }
        LOGGER.info("Creating EventForwarderMqtt for MQTT version " + version);
        client = clientFactory.create(version, host, port, username, password, topic);
        this.objectMapper = objectMapper;
    }

    @Override
    public void forward(EventData eventData, ResultHandler resultHandler) {
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsString(eventData).getBytes();
        } catch (JsonProcessingException e) {
            resultHandler.onResult(false, e);
            return;
        }

        client.publish(payload, resultHandler);
    }

}
