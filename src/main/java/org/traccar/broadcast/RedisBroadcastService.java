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
package org.traccar.broadcast;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.BaseModel;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Permission;
import org.traccar.model.Position;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class RedisBroadcastService implements BroadcastService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisBroadcastService.class);

    private final ObjectMapper objectMapper;

    private final ExecutorService service = Executors.newSingleThreadExecutor();

    private final Set<BroadcastInterface> listeners = new HashSet<>();

    private final String url;
    private final String pubsubChannel = "traccar:cast";

    private final Jedis subscriberJedis;
    private final Jedis publisherJedis;

    private final String id;

    public RedisBroadcastService(Config config, ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper;
        url = config.getString(Keys.BROADCAST_ADDRESS);

        subscriberJedis = new Jedis(url);
        publisherJedis = new Jedis(url);

        // id that will be used to identify this instance of the server
        id = String.valueOf(System.currentTimeMillis());
    }

    @Override
    public boolean singleInstance() {
        return false;
    }

    @Override
    public void registerListener(BroadcastInterface listener) {
        listeners.add(listener);
    }

    @Override
    public void updateDevice(boolean local, Device device) {
        BroadcastMessage message = new BroadcastMessage();
        message.setDevice(device);
        sendMessage(message);
    }

    @Override
    public void updatePosition(boolean local, Position position) {
        BroadcastMessage message = new BroadcastMessage();
        message.setPosition(position);
        sendMessage(message);
    }

    @Override
    public void updateEvent(boolean local, long userId, Event event) {
        BroadcastMessage message = new BroadcastMessage();
        message.setUserId(userId);
        message.setEvent(event);
        sendMessage(message);
    }

    @Override
    public void updateCommand(boolean local, long deviceId) {
        BroadcastMessage message = new BroadcastMessage();
        message.setCommandDeviceId(deviceId);
        sendMessage(message);
    }

    @Override
    public void invalidateObject(boolean local, Class<? extends BaseModel> clazz, long id) {
        BroadcastMessage message = new BroadcastMessage();
        message.setChanges(Map.of(Permission.getKey(clazz), id));
        sendMessage(message);
    }

    @Override
    public void invalidatePermission(
            boolean local,
            Class<? extends BaseModel> clazz1, long id1,
            Class<? extends BaseModel> clazz2, long id2) {
        BroadcastMessage message = new BroadcastMessage();
        message.setChanges(Map.of(Permission.getKey(clazz1), id1, Permission.getKey(clazz2), id2));
        sendMessage(message);
    }

    private void sendMessage(BroadcastMessage message) {
        try {
            String payload = id  + ":" + objectMapper.writeValueAsString(message);
            publisherJedis.publish(pubsubChannel, payload);
        } catch (IOException e) {
            LOGGER.warn("Broadcast failed", e);
        }
    }

    private void handleMessage(BroadcastMessage message) {
        if (message.getDevice() != null) {
            listeners.forEach(listener -> listener.updateDevice(false, message.getDevice()));
        } else if (message.getPosition() != null) {
            listeners.forEach(listener -> listener.updatePosition(false, message.getPosition()));
        } else if (message.getUserId() != null && message.getEvent() != null) {
            listeners.forEach(listener -> listener.updateEvent(false, message.getUserId(), message.getEvent()));
        } else if (message.getCommandDeviceId() != null) {
            listeners.forEach(listener -> listener.updateCommand(false, message.getCommandDeviceId()));
        } else if (message.getChanges() != null) {
            var iterator = message.getChanges().entrySet().iterator();
            if (iterator.hasNext()) {
                var first = iterator.next();
                if (iterator.hasNext()) {
                    var second = iterator.next();
                    listeners.forEach(listener -> listener.invalidatePermission(
                            false,
                            Permission.getKeyClass(first.getKey()), first.getValue(),
                            Permission.getKeyClass(second.getKey()), second.getValue()));
                } else {
                    listeners.forEach(listener -> listener.invalidateObject(
                            false,
                            Permission.getKeyClass(first.getKey()), first.getValue()));
                }
            }
        }
    }

    @Override
    public void start() throws IOException {
        service.submit(receiver);
    }

    @Override
    public void stop() {
        service.shutdown();
    }

    private final Runnable receiver = new Runnable() {
        @Override
        public void run() {
            subscriberJedis.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    try {
                        String[] parts = message.split(":", 2);
                        if (channel == pubsubChannel && parts.length == 2 && !id.equals(parts[0])) {
                            handleMessage(objectMapper.readValue(parts[1], BroadcastMessage.class));
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Broadcast handleMessage failed", e);
                    }
                }
            }, pubsubChannel);

            while (!service.isShutdown()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }

            try {
                subscriberJedis.close();
                publisherJedis.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close pubsub", e);
                throw new RuntimeException(e);
            }
        }
    };

}
