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
import org.traccar.LifecycleObject;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import javax.inject.Inject;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BroadcastService implements LifecycleObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastService.class);

    private final ObjectMapper objectMapper;

    private final InetAddress address;
    private final int port;

    private DatagramSocket publisherSocket;

    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final byte[] receiverBuffer = new byte[4096];

    @Inject
    public BroadcastService(Config config, ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper;
        address = InetAddress.getByName(config.getString(Keys.BROADCAST_ADDRESS));
        port = config.getInteger(Keys.BROADCAST_PORT);
    }

    public void sendMessage(BroadcastMessage message) throws IOException {
        byte[] buffer = objectMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        publisherSocket.send(packet);
    }

    private void handleMessage(BroadcastMessage message) {
        if (message.getDeviceStatus() != null) {
            LOGGER.info("Broadcast received device {}", message.getDeviceStatus().getDeviceId());
        } else if (message.getPosition() != null) {
            LOGGER.info("Broadcast received position {}", message.getPosition().getDeviceId());
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
                socket.joinGroup(address);
                while (!service.isShutdown()) {
                    DatagramPacket packet = new DatagramPacket(receiverBuffer, receiverBuffer.length);
                    socket.receive(packet);
                    String data = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    handleMessage(objectMapper.readValue(data, BroadcastMessage.class));
                }
                socket.leaveGroup(address);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };

}
