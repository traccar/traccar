/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.traccar.config.Config;
import org.traccar.config.Keys;

import java.io.IOException;

public class PositionForwarderAmqp implements PositionForwarder {

    private final AmqpClient amqpClient;
    private final ObjectMapper objectMapper;

    public PositionForwarderAmqp(Config config, ObjectMapper objectMapper) {
        String connectionUrl = config.getString(Keys.FORWARD_URL);
        String exchange = config.getString(Keys.FORWARD_EXCHANGE);
        String topic = config.getString(Keys.FORWARD_TOPIC);
        amqpClient = new AmqpClient(connectionUrl, exchange, topic);
        this.objectMapper = objectMapper;
    }

    @Override
    public void forward(PositionData positionData, ResultHandler resultHandler) {
        try {
            String value = objectMapper.writeValueAsString(positionData);
            amqpClient.publishMessage(value);
            resultHandler.onResult(true, null);
        } catch (IOException e) {
            resultHandler.onResult(false, e);
        }
    }
}
