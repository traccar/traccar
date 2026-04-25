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
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.database.DeviceLookupService;
import org.traccar.helper.BitUtil;
import org.traccar.media.VideoStreamManager;

import jakarta.inject.Inject;
import java.net.SocketAddress;

public class Jt1078ProtocolDecoder extends BaseProtocolDecoder {

    private DeviceLookupService deviceLookupService;
    private VideoStreamManager streamManager;

    private CompositeByteBuf frameBuffer;
    private int frameDataType;
    private long frameTimestamp;
    private int framePayloadType;

    private String streamUniqueId;
    private int streamChannel;

    public Jt1078ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Inject
    public void setDeviceLookupService(DeviceLookupService deviceLookupService) {
        this.deviceLookupService = deviceLookupService;
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
        int payloadType = buf.readUnsignedByte() & 0x7F; // M/PT
        buf.readUnsignedShort(); // index

        String uniqueId = Jt808ProtocolDecoder.decodeId(buf.readSlice(6));
        int videoChannel = buf.readUnsignedByte();
        int rawType = buf.readUnsignedByte();
        int dataType = BitUtil.from(rawType, 4);
        int subpackageType = BitUtil.to(rawType, 4);
        long timestamp = buf.readLong();

        if (dataType <= 2) {
            buf.skipBytes(4); // i-frame interval + frame interval
        }

        int bodyLength = buf.readUnsignedShort();

        if (bodyLength == 0 || dataType > 2) {
            return null;
        }

        if (deviceLookupService.lookup(new String[]{uniqueId}) == null) {
            return null;
        }

        streamUniqueId = uniqueId;
        streamChannel = videoChannel;

        ByteBuf body = buf.readRetainedSlice(bodyLength);

        if (subpackageType == 0) {
            boolean isKeyFrame = dataType == 0;
            streamManager.handleFrame(uniqueId, videoChannel, body, timestamp, isKeyFrame, payloadType);
            body.release();
        } else if (subpackageType == 1) {
            if (frameBuffer != null) {
                frameBuffer.release();
            }
            frameBuffer = Unpooled.compositeBuffer();
            frameBuffer.addComponent(true, body);
            frameDataType = dataType;
            frameTimestamp = timestamp;
            framePayloadType = payloadType;
        } else if (subpackageType == 3) {
            if (frameBuffer != null) {
                frameBuffer.addComponent(true, body);
            } else {
                body.release();
            }
        } else if (subpackageType == 2) {
            if (frameBuffer != null) {
                frameBuffer.addComponent(true, body);
                boolean isKeyFrame = frameDataType == 0;
                streamManager.handleFrame(
                        uniqueId, videoChannel, frameBuffer, frameTimestamp, isKeyFrame, framePayloadType);
                frameBuffer.release();
                frameBuffer = null;
            } else {
                body.release();
            }
        }

        return null;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (streamUniqueId != null) {
            streamManager.removeStream(streamUniqueId, streamChannel);
        }
        if (frameBuffer != null) {
            frameBuffer.release();
            frameBuffer = null;
        }
    }

}
