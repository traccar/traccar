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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

public class AmqpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpClient.class);

    private final ConnectionFactory factory;
    private final String exchange;
    private final String topic;

    private Channel channel;

    AmqpClient(String connectionUrl, String exchange, String topic) {
        this.exchange = exchange;
        this.topic = topic;

        factory = new ConnectionFactory();
        try {
            factory.setUri(connectionUrl);
        } catch (NoSuchAlgorithmException | URISyntaxException | KeyManagementException e) {
            throw new RuntimeException("Error while setting URI for RabbitMQ connection factory", e);
        }
    }

    private void connect() throws IOException, TimeoutException {
        Connection connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, true);
    }

    public synchronized void publishMessage(String message) throws IOException {
        if (channel == null || !channel.isOpen()) {
            try {
                connect();
            } catch (IOException | TimeoutException e) {
                channel = null;
                LOGGER.warn("AMQP connection error", e);
                return;
            }
        }
        try {
            channel.basicPublish(
                    exchange, topic, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            channel = null;
            throw e;
        }
    }
}
