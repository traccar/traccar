package org.traccar.notificators;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.notification.MessageException;

import java.util.Properties;

public class NotificatorKafka extends Notificator {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorKafka.class);

    private final Producer<String, String> producer;

    public NotificatorKafka() {
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
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        producer = new KafkaProducer<>(props);
    }

    @Override
    public void sendSync(long userId, Event event, Position position) throws MessageException, InterruptedException {

    }
}
