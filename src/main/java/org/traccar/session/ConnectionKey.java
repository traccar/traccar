/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
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

import java.net.SocketAddress;
import java.util.Objects;

public class ConnectionKey {

    private final SocketAddress localAddress;
    private final SocketAddress remoteAddress;

    public ConnectionKey(Channel channel, SocketAddress remoteAddress) {
        this(channel.localAddress(), remoteAddress);
    }

    public ConnectionKey(SocketAddress localAddress, SocketAddress remoteAddress) {
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConnectionKey that = (ConnectionKey) o;
        return Objects.equals(localAddress, that.localAddress) && Objects.equals(remoteAddress, that.remoteAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localAddress, remoteAddress);
    }

}
