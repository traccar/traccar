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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MulticastBroadcastService extends BaseBroadcastService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MulticastBroadcastService.class);

    private final ObjectMapper objectMapper;

    private final NetworkInterface networkInterface;
    private final int port;
    private final InetSocketAddress group;

    private DatagramSocket publisherSocket;

    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final byte[] receiverBuffer = new byte[4096];

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
    public boolean singleInstance() {
        return false;
    }

    @Override
    protected void sendMessage(BroadcastMessage message) {
        try {
            byte[] buffer = objectMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group);
            publisherSocket.send(packet);
        } catch (IOException e) {
            LOGGER.warn("Broadcast failed", e);
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
            try (MulticastSocket socket = new MulticastSocket(port)) {
                socket.setNetworkInterface(networkInterface);
                socket.joinGroup(group, networkInterface);
                publisherSocket = socket;
                while (!service.isShutdown()) {
                    DatagramPacket packet = new DatagramPacket(receiverBuffer, receiverBuffer.length);
                    socket.receive(packet);
                    if (networkInterface.inetAddresses().noneMatch(a -> a.equals(packet.getAddress()))) {
                        String data = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                        handleMessage(objectMapper.readValue(data, BroadcastMessage.class));
                    }
                }
                publisherSocket = null;
                socket.leaveGroup(group, networkInterface);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

}
