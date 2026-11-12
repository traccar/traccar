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
import io.netty.handler.codec.TooLongFrameException;
import org.traccar.BaseMediaProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.media.VideoStreamManager;
import org.traccar.model.Pair;
import org.traccar.session.DeviceSession;

import jakarta.inject.Inject;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

public class Jt1078ProtocolDecoder extends BaseMediaProtocolDecoder {

    private static final int MAX_FRAME_SIZE = 10 * 1024 * 1024;

    private VideoStreamManager streamManager;

    private CompositeByteBuf frameBuffer;
    private int frameDataType;
    private long frameTimestamp;
    private int framePayloadType;

    private final Set<Pair<Long, Integer>> streams = new HashSet<>();

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
        int payloadType = buf.readUnsignedByte() & 0x7F; // M/PT
        buf.readUnsignedShort(); // index

        int idLength = buf.getUnsignedShort(buf.readerIndex()) == 0 ? 10 : 6;
        String uniqueId = Jt808ProtocolDecoder.decodeId(buf.readSlice(idLength));
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

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, uniqueId);
        if (deviceSession == null) {
            return null;
        }

        long streamDeviceId = deviceSession.getDeviceId();
        streams.add(new Pair<>(streamDeviceId, videoChannel));

        ByteBuf body = buf.readRetainedSlice(bodyLength);

        try {
            if (subpackageType == 0) {
                boolean isKeyFrame = dataType == 0;
                streamManager.handleFrame(streamDeviceId, videoChannel, body, timestamp, isKeyFrame, payloadType);
            } else if (subpackageType == 1) {
                if (frameBuffer != null) {
                    frameBuffer.release();
                }
                frameBuffer = Unpooled.compositeBuffer();
                frameBuffer.addComponent(true, body.retain());
                frameDataType = dataType;
                frameTimestamp = timestamp;
                framePayloadType = payloadType;
            } else if (subpackageType == 3 || subpackageType == 2) {
                if (frameBuffer != null) {
                    if (bodyLength > MAX_FRAME_SIZE - frameBuffer.readableBytes()) {
                        throw new TooLongFrameException();
                    }
                    frameBuffer.addComponent(true, body.retain());
                    if (subpackageType == 2) {
                        boolean isKeyFrame = frameDataType == 0;
                        try {
                            streamManager.handleFrame(
                                    streamDeviceId, videoChannel, frameBuffer,
                                    frameTimestamp, isKeyFrame, framePayloadType);
                        } finally {
                            frameBuffer.release();
                            frameBuffer = null;
                        }
                    }
                }
            }
        } catch (TooLongFrameException e) {
            channel.close();
            throw e;
        } finally {
            body.release();
        }

        return null;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        for (Pair<Long, Integer> stream : streams) {
            streamManager.removeStream(stream.first(), stream.second());
        }
        streams.clear();
        if (frameBuffer != null) {
            frameBuffer.release();
            frameBuffer = null;
        }
    }

}
