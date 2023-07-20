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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public class EventForwarderAmqp implements EventForwarder {

    private final Channel channel;
    private final ObjectMapper objectMapper;

    private final String exchange;
    private final String topic;

    public EventForwarderAmqp(Config config, ObjectMapper objectMapper) {
        ConnectionFactory factory = new ConnectionFactory();
        try {
            factory.setUri(config.getString(Keys.EVENT_FORWARD_URL));
        } catch (NoSuchAlgorithmException | URISyntaxException | KeyManagementException e) {
            throw new RuntimeException(e);
        }

        try {
            Connection connection  = factory.newConnection();
            exchange = config.getString(Keys.EVENT_FORWARD_EXCHANGE);
            topic = config.getString(Keys.EVENT_FORWARD_TOPIC);
            channel = connection.createChannel();
            channel.exchangeDeclare(exchange, "topic", true);
            this.objectMapper = objectMapper;
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void forward(EventData eventData, ResultHandler resultHandler) {
        try {
            String value = objectMapper.writeValueAsString(eventData);

            BasicProperties properties = new BasicProperties.Builder()
                    .contentType("application/json")
                    .contentEncoding("string")
                    .deliveryMode(2)
                    .priority(10)
                    .build();

            channel.basicPublish(exchange, topic, properties, value.getBytes());
            resultHandler.onResult(true, null);
        } catch (IOException e) {
            resultHandler.onResult(false, e);
        }
    }
}
