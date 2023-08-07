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

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

public class AmqpClient {
    private final Channel channel;
    private final String exchange;
    private final String topic;

    AmqpClient(String connectionUrl, String exchange, String topic) {
        this.exchange = exchange;
        this.topic = topic;

        ConnectionFactory factory = new ConnectionFactory();
        try {
            factory.setUri(connectionUrl);
        } catch (NoSuchAlgorithmException | URISyntaxException | KeyManagementException e) {
            throw new RuntimeException("Error while setting URI for RabbitMQ connection factory", e);
        }

        try {
            Connection connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, true);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Error while creating and configuring RabbitMQ channel", e);
        }
    }

    public void publishMessage(String message) throws IOException {
        channel.basicPublish(exchange, topic, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
    }
}
