/*
 * Copyright 2018 - 2026 Anton Tananaev (anton@traccar.org)
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
package org.traccar;

import io.netty.util.ReferenceCounted;
import io.netty.util.ReferenceCountUtil;

import java.net.SocketAddress;

public class NetworkMessage implements ReferenceCounted {

    private final SocketAddress remoteAddress;
    private final Object message;

    public NetworkMessage(Object message, SocketAddress remoteAddress) {
        this.message = message;
        this.remoteAddress = remoteAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public Object getMessage() {
        return message;
    }

    @Override
    public int refCnt() {
        return message instanceof ReferenceCounted resource ? resource.refCnt() : 1;
    }

    @Override
    public NetworkMessage retain() {
        ReferenceCountUtil.retain(message);
        return this;
    }

    @Override
    public NetworkMessage retain(int increment) {
        ReferenceCountUtil.retain(message, increment);
        return this;
    }

    @Override
    public NetworkMessage touch() {
        ReferenceCountUtil.touch(message);
        return this;
    }

    @Override
    public NetworkMessage touch(Object hint) {
        ReferenceCountUtil.touch(message, hint);
        return this;
    }

    @Override
    public boolean release() {
        return ReferenceCountUtil.release(message);
    }

    @Override
    public boolean release(int decrement) {
        return ReferenceCountUtil.release(message, decrement);
    }

}
