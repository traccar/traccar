/*
 * Copyright 2023 - 2026 Anton Tananaev (anton@traccar.org)
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
package org.traccar.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.netty.channel.Channel;
import org.traccar.session.ConnectionKey;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class LogRecord {

    private final ConnectionKey connectionKey;
    private final InetSocketAddress remoteAddress;

    public LogRecord(Channel channel, SocketAddress remoteAddress) {
        this.connectionKey = new ConnectionKey(channel, remoteAddress);
        this.remoteAddress = (InetSocketAddress) remoteAddress;
    }

    @JsonIgnore
    public InetSocketAddress getAddress() {
        return remoteAddress;
    }

    @JsonIgnore
    public ConnectionKey getConnectionKey() {
        return connectionKey;
    }

    public String getHost() {
        return remoteAddress.getHostString();
    }

    private String protocol;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    private String uniqueId;

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    private long deviceId;

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
