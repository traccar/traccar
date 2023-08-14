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
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.auth.Mqtt3SimpleAuth;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class MqttClientV3 implements MqttClient {
    private final Mqtt3AsyncClient client;
    private final String topic;

    public MqttClientV3(final String host, int port, String user, String password, String topic) {
        Mqtt3SimpleAuth simpleAuth = null;
        if (user != null && password != null) {
            simpleAuth = Mqtt3SimpleAuth.builder()
                    .username(user)
                    .password(password.getBytes())
                    .build();
        }
        client = Mqtt3Client.builder()
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
        this.topic = topic;
    }

    @Override
    public void publish(final byte[] payload, ResultHandler resultHandler) {
        client.publishWith()
                .topic(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(payload)
                .send()
                .whenComplete((message, e) -> resultHandler.onResult(e == null, e));
    }
}
