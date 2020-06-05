package org.traccar.notificators;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.*;
import org.traccar.notification.MessageException;

import java.util.Properties;

public class NotificatorKafka extends Notificator {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorKafka.class);

    private final String topic;
    private final Producer<Long, String> producer;
    private ObjectMapper objectMapper;

    public NotificatorKafka() {
        topic = Context.getConfig().getString("notificator.kafka.topic", "traccar");
        String bootstrapServers = Context.getConfig().getString("notificator.kafka.bootstrap.servers", "localhost:4242");
        String acks = Context.getConfig().getString("notificator.kafka.acks", "all");
        int retries = Context.getConfig().getInteger("notificator.kafka.retries", 0);
        int batchSize = Context.getConfig().getInteger("notificator.kafka.batch.size", 16384);
        int lingerMs = Context.getConfig().getInteger("notificator.kafka.linger.ms", 1);
        int bufferMemory = Context.getConfig().getInteger("notificator.kafka.buffer.memory", 33554432);

        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("acks", acks);
        props.put("retries", retries);
        props.put("batch.size", batchSize);
        props.put("linger.ms", lingerMs);
        props.put("buffer.memory", bufferMemory);
        props.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        producer = new KafkaProducer<>(props);

        objectMapper = new ObjectMapper();
    }

    public static class Message {
        long userId;
        Event event;
        Position position;
    }

    @Override
    public void sendSync(long userId, Event event, Position position) throws MessageException, InterruptedException {
        Message message = new Message();
        message.userId = userId;
        message.event = event;
        message.position = position;
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            LOGGER.warn("Could not serialize json", ex);
            throw new MessageException(ex);
        }

        producer.send(new ProducerRecord<>(topic, userId, json));
    }
}
