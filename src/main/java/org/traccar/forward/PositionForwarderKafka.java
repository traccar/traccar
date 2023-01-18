/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import java.util.Properties;

public class PositionForwarderKafka implements PositionForwarder {

    private final Producer<String, String> producer;
    private final ObjectMapper objectMapper;

    private final String topic;

    public PositionForwarderKafka(Config config, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        Properties properties = new Properties();
        properties.put("bootstrap.servers", config.getString(Keys.FORWARD_URL));
        properties.put("acks", "all");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(properties);
        topic = config.getString(Keys.FORWARD_TOPIC);
    }

    @Override
    public void forward(PositionData positionData, ResultHandler resultHandler) {
        try {
            String key = Long.toString(positionData.getDevice().getId());
            String value = objectMapper.writeValueAsString(positionData);
            producer.send(new ProducerRecord<>(topic, key, value));
            resultHandler.onResult(true, null);
        } catch (JsonProcessingException e) {
            resultHandler.onResult(false, e);
        }
    }

}
