/*
 * Copyright 2023 - 2024 Anton Tananaev (anton@traccar.org)
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
package org.traccar.broadcast;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public class RedisBroadcastService extends BaseBroadcastService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisBroadcastService.class);

    private final ObjectMapper objectMapper;

    private final ExecutorService executorService;

    private final String channel = "traccar";

    private Jedis subscriber;
    private Jedis publisher;

    private final String id = UUID.randomUUID().toString();

    public RedisBroadcastService(
            Config config, ExecutorService executorService, ObjectMapper objectMapper) throws IOException {
        this.executorService = executorService;
        this.objectMapper = objectMapper;
        String url = config.getString(Keys.BROADCAST_ADDRESS);

        try {
            subscriber = new Jedis(url);
            publisher = new Jedis(url);
            subscriber.connect();
        } catch (JedisConnectionException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean singleInstance() {
        return false;
    }

    @Override
    protected void sendMessage(BroadcastMessage message) {
        try {
            String payload = id  + ":" + objectMapper.writeValueAsString(message);
            publisher.publish(channel, payload);
        } catch (IOException | JedisConnectionException e) {
            LOGGER.warn("Broadcast failed", e);
        }
    }

    @Override
    public void start() throws IOException {
        executorService.submit(receiver);
    }

    @Override
    public void stop() {
        try {
            if (subscriber != null) {
                subscriber.close();
                subscriber = null;
            }
        } catch (JedisException e) {
            LOGGER.warn("Subscriber close failed", e);
        }
        try {
            if (publisher != null) {
                publisher.close();
                publisher = null;
            }
        } catch (JedisException e) {
            LOGGER.warn("Publisher close failed", e);
        }
    }

    private final Runnable receiver = new Runnable() {
        @Override
        public void run() {
            try {
                subscriber.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String messageChannel, String message) {
                        try {
                            String[] parts = message.split(":", 2);
                            if (messageChannel.equals(channel) && parts.length == 2 && !id.equals(parts[0])) {
                                handleMessage(objectMapper.readValue(parts[1], BroadcastMessage.class));
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Broadcast handleMessage failed", e);
                        }
                    }
                }, channel);
            } catch (JedisException e) {
                throw new RuntimeException(e);
            }
        }
    };

}
