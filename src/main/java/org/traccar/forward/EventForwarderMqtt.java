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

import org.traccar.config.Config;
import org.traccar.config.Keys;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventForwarderMqtt implements EventForwarder {

    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    private final String topic;

    public EventForwarderMqtt(Config config, ObjectMapper objectMapper) {
        this.topic = config.getString(Keys.EVENT_FORWARD_TOPIC);
        mqttClient = new MqttClient(config.getString(Keys.EVENT_FORWARD_URL));
        this.objectMapper = objectMapper;
    }

    @Override
    public void forward(EventData eventData, ResultHandler resultHandler) {
        try {
            String payload = objectMapper.writeValueAsString(eventData);
            mqttClient.publish(topic, payload, (message, e) -> resultHandler.onResult(e == null, e));
        } catch (JsonProcessingException e) {
            resultHandler.onResult(false, e);
        }
    }

}
