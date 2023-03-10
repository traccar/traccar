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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class EventForwarderMqtt implements EventForwarder {

    private final Mqtt5AsyncClient client;
    private final ObjectMapper objectMapper;

    private final String topic;

    public EventForwarderMqtt(Config config, ObjectMapper objectMapper) {
        URI url;
        try {
            url = new URI(config.getString(Keys.EVENT_FORWARD_URL));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        String userInfo = url.getUserInfo();
        Mqtt5SimpleAuth simpleAuth = null;
        if (userInfo != null) {
            int delimiter = userInfo.indexOf(':');
            if (delimiter == -1) {
                throw new IllegalArgumentException("Wrong credentials. Should be in format \"username:password\"");
            } else {
                simpleAuth = Mqtt5SimpleAuth.builder()
                        .username(userInfo.substring(0, delimiter++))
                        .password(userInfo.substring(delimiter).getBytes())
                        .build();
            }
        }

        String host = url.getHost();
        int port = url.getPort();
        client = Mqtt5Client.builder()
                .identifier("traccar-" + UUID.randomUUID())
                .serverHost(host)
                .serverPort(port)
                .simpleAuth(simpleAuth)
                .automaticReconnectWithDefaultConfig()
                .buildAsync();

        client.connectWith()
                .send()
                .whenComplete((message, e) -> {
                    if (e != null) {
                        throw new RuntimeException(e);
                    }
                });

        this.objectMapper = objectMapper;
        topic = config.getString(Keys.EVENT_FORWARD_TOPIC);
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

        client.publishWith()
                .topic(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(payload)
                .send()
                .whenComplete((message, e) -> resultHandler.onResult(e == null, e));
    }

}
