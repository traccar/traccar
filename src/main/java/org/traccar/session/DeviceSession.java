/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.session;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import org.traccar.BasePipelineFactory;
import org.traccar.Protocol;
import org.traccar.model.Command;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

public class DeviceSession {

    private final long deviceId;
    private final String uniqueId;
    private final Protocol protocol;
    private final Channel channel;
    private final SocketAddress remoteAddress;

    public DeviceSession(
            long deviceId, String uniqueId, Protocol protocol, Channel channel, SocketAddress remoteAddress) {
        this.deviceId = deviceId;
        this.uniqueId = uniqueId;
        this.protocol = protocol;
        this.channel = channel;
        this.remoteAddress = remoteAddress;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public Channel getChannel() {
        return channel;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public boolean supportsLiveCommands() {
        return BasePipelineFactory.getHandler(channel.pipeline(), HttpRequestDecoder.class) == null;
    }

    public void sendCommand(Command command) {
        protocol.sendDataCommand(channel, remoteAddress, command);
    }

    public static final String KEY_TIMEZONE = "timezone";

    private final Map<String, Object> locals = new HashMap<>();

    public boolean contains(String key) {
        return locals.containsKey(key);
    }

    public void set(String key, Object value) {
        if (value != null) {
            locals.put(key, value);
        } else {
            locals.remove(key);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) locals.get(key);
    }

}
