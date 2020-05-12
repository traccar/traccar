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
    private static final String EXCHANGE_NAME = "traccar";
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
        factory.setUri(config.getString(Keys.RABBITMQ_CONNECTION_URL));
        this.connection = factory.newConnection();
        this.channel = connection.createChannel();
        this.channel.exchangeDeclare(EXCHANGE_NAME, "direct", true);
    }

    public void sendToPositionsQueue(String message) throws IOException {
        channel.basicPublish(EXCHANGE_NAME, config.getString(Keys.RABBITMQ_ROUTING_KEY_POSITIONS), MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
    }

    public void sendToEventsQueue(String message) throws IOException {
        channel.basicPublish(EXCHANGE_NAME, config.getString(Keys.RABBITMQ_ROUTING_KEY_EVENTS), MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
    }
}
