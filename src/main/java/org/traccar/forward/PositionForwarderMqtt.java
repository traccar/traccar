/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
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

public class PositionForwarderMqtt implements PositionForwarder {

    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    private final String topic;

    public PositionForwarderMqtt(final Config config, final ObjectMapper objectMapper) {
        this.topic = config.getString(Keys.FORWARD_TOPIC);
        mqttClient = new MqttClient(config.getString(Keys.FORWARD_URL));
        this.objectMapper = objectMapper;
    }

    @Override
    public void forward(PositionData positionData, ResultHandler resultHandler) {
        try {
            String payload = objectMapper.writeValueAsString(topic);
            mqttClient.publish(topic, payload, (message, e) -> resultHandler.onResult(e == null, e));
        } catch (JsonProcessingException e) {
            resultHandler.onResult(false, e);
        }
    }

}
