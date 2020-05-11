package org.traccar.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RabbitmqManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitmqManager.class);
    private final Config config;
    private Connection connection;
    private Channel channel;


    public RabbitmqManager(Config config) throws Exception {
        this.config = config;
        initRabbitmq();
    }

    private void initRabbitmq() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.getString(Keys.RABBITMQ_HOST_NAME));
        this.connection = factory.newConnection();
        this.channel = connection.createChannel();
        this.channel.queueDeclare(this.config.getString(Keys.RABBITMQ_POSITIONS_QUEUE_NAME), true, false, false, null);
        this.channel.queueDeclare(this.config.getString(Keys.RABBITMQ_EVENTS_QUEUE_NAME), true, false, false, null);
    }

    public void sendToPositionsQueue(String message) throws IOException {
        channel.basicPublish("", this.config.getString(Keys.RABBITMQ_POSITIONS_QUEUE_NAME), MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
    }

    public void sendToEventsQueue(String message) throws IOException {
        channel.basicPublish("", this.config.getString(Keys.RABBITMQ_EVENTS_QUEUE_NAME), MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
    }
}
