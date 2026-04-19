/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.media.VideoStreamManager;

import jakarta.inject.Inject;
import java.net.SocketAddress;

public class Jt1078ProtocolDecoder extends BaseProtocolDecoder {

    private VideoStreamManager streamManager;

    public Jt1078ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Inject
    public void setStreamManager(VideoStreamManager streamManager) {
        this.streamManager = streamManager;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedInt(); // header
        buf.readUnsignedByte(); // V/P/X/CC
        buf.readUnsignedByte(); // M/PT
        buf.readUnsignedShort(); // index

        String uniqueId = HuabaoProtocolDecoder.decodeId(buf.readSlice(6));
        int logicalChannel = buf.readUnsignedByte();
        int dataType = BitUtil.from(buf.readUnsignedByte(), 4);
        long timestamp = buf.readLong();

        if (dataType <= 2) {
            buf.skipBytes(4); // i-frame interval + frame interval
        }

        int bodyLength = buf.readUnsignedShort();

        if (bodyLength == 0 || dataType > 2) {
            return null; // skip audio and transparent data
        }

        boolean isKeyFrame = dataType == 0; // i-frame

        getDeviceSession(channel, remoteAddress, uniqueId);

        streamManager.handleFrame(uniqueId, logicalChannel, buf.readSlice(bodyLength), timestamp, isKeyFrame);

        return null;
    }

}
