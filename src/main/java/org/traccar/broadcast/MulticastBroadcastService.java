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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MulticastBroadcastService implements BroadcastService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MulticastBroadcastService.class);

    private final ObjectMapper objectMapper;

    private final NetworkInterface networkInterface;
    private final int port;
    private final InetSocketAddress group;

    private DatagramSocket publisherSocket;

    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final byte[] receiverBuffer = new byte[4096];

    private final Set<BroadcastInterface> listeners = new HashSet<>();

    public MulticastBroadcastService(Config config, ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper;
        port = config.getInteger(Keys.BROADCAST_PORT);
        String interfaceName = config.getString(Keys.BROADCAST_INTERFACE);
        if (interfaceName.indexOf('.') >= 0 || interfaceName.indexOf(':') >= 0) {
            networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(interfaceName));
        } else {
            networkInterface = NetworkInterface.getByName(interfaceName);
        }
        InetAddress address = InetAddress.getByName(config.getString(Keys.BROADCAST_ADDRESS));
        group = new InetSocketAddress(address, port);
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
    public void invalidateObject(Class<? extends BaseModel> clazz, long id) {
        BroadcastMessage message = new BroadcastMessage();
        message.setChanges(Map.of(Permission.getKey(clazz), id));
        sendMessage(message);
    }

    @Override
    public void invalidatePermission(
            Class<? extends BaseModel> clazz1, long id1,
            Class<? extends BaseModel> clazz2, long id2) {
        BroadcastMessage message = new BroadcastMessage();
        message.setChanges(Map.of(Permission.getKey(clazz1), id1, Permission.getKey(clazz2), id2));
        sendMessage(message);
    }

    private void sendMessage(BroadcastMessage message) {
        try {
            byte[] buffer = objectMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group);
            publisherSocket.send(packet);
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
        } else if (message.getChanges() != null) {
            var iterator = message.getChanges().entrySet().iterator();
            if (iterator.hasNext()) {
                var first = iterator.next();
                if (iterator.hasNext()) {
                    var second = iterator.next();
                    listeners.forEach(listener -> listener.invalidatePermission(
                            Permission.getKeyClass(first.getKey()), first.getValue(),
                            Permission.getKeyClass(second.getKey()), second.getValue()));
                } else {
                    listeners.forEach(listener -> listener.invalidateObject(
                            Permission.getKeyClass(first.getKey()), first.getValue()));
                }
            }
        }
    }

    @Override
    public void start() throws IOException {
        service.submit(receiver);
        publisherSocket = new DatagramSocket();
    }

    @Override
    public void stop() {
        publisherSocket.close();
        service.shutdown();
    }

    private final Runnable receiver = new Runnable() {
        @Override
        public void run() {
            try (MulticastSocket socket = new MulticastSocket(port)) {
                socket.joinGroup(group, networkInterface);
                while (!service.isShutdown()) {
                    DatagramPacket packet = new DatagramPacket(receiverBuffer, receiverBuffer.length);
                    socket.receive(packet);
                    String data = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    LOGGER.info("Broadcast received: {}", data);
                    handleMessage(objectMapper.readValue(data, BroadcastMessage.class));
                }
                socket.leaveGroup(group, networkInterface);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };

}
