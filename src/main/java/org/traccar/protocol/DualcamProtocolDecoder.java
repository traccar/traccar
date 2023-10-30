/*
 * Copyright 2021 - 2023 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.model.Device;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class DualcamProtocolDecoder extends BaseProtocolDecoder {

    public DualcamProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_INIT = 0;
    public static final int MSG_START = 1;
    public static final int MSG_RESUME = 2;
    public static final int MSG_SYNC = 3;
    public static final int MSG_DATA = 4;
    public static final int MSG_COMPLETE = 5;
    public static final int MSG_FILE_REQUEST = 8;
    public static final int MSG_INIT_REQUEST = 9;
    public static final int MSG_PATH_REQUEST = 0x000C;
    public static final int MSG_PATH_RESPONSE = 0x000D;

    private String model;
    private String uniqueId;
    private int dataSize;
    private int dataCurrent;
    private boolean video;
    private ByteBuf media;

    private boolean isPacketData() {
        if (model == null) {
            return dataSize < 8192;
        } else {
            return !"DSM".equals(model);
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int type = buf.readUnsignedShort();

        DeviceSession deviceSession;
        switch (type) {
            case MSG_INIT:
                buf.readUnsignedShort(); // protocol id
                uniqueId = String.valueOf(buf.readLong());
                deviceSession = getDeviceSession(channel, remoteAddress, uniqueId);
                long settings = buf.readUnsignedInt();
                if (channel != null && deviceSession != null) {
                    model = getCacheManager().getObject(Device.class, deviceSession.getDeviceId()).getModel();
                    ByteBuf response = Unpooled.buffer();
                    if (BitUtil.check(settings, 25)) {
                        response.writeShort(MSG_PATH_REQUEST);
                        response.writeShort(2);
                        response.writeShort(0);
                    } else if (BitUtil.between(settings, 26, 30) > 0) {
                        response.writeShort(MSG_FILE_REQUEST);
                        String file;
                        if (BitUtil.check(settings, 26)) {
                            video = false;
                            file = "%photof";
                        } else if (BitUtil.check(settings, 27)) {
                            video = false;
                            file = "%photor";
                        } else if (BitUtil.check(settings, 28)) {
                            video = true;
                            file = "%videof";
                        } else {
                            video = true;
                            file = "%videor";
                        }
                        response.writeShort(file.length());
                        response.writeCharSequence(file, StandardCharsets.US_ASCII);
                    } else {
                        response.writeShort(MSG_INIT);
                    }
                    channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
                }
                break;
            case MSG_START:
                buf.readUnsignedShort(); // length
                dataSize = buf.readInt();
                dataCurrent = isPacketData() ? 1 : 0;
                media = Unpooled.buffer();
                if (channel != null) {
                    ByteBuf response = Unpooled.buffer();
                    response.writeShort(MSG_RESUME);
                    response.writeShort(4);
                    response.writeInt(dataCurrent);
                    channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
                }
                break;
            case MSG_DATA:
                int length = buf.readUnsignedShort() - 2;
                media.writeBytes(buf, length);
                boolean finished;
                if (isPacketData()) {
                    finished = dataCurrent == dataSize;
                    dataCurrent += 1;
                } else {
                    finished = dataCurrent + length == dataSize;
                    dataCurrent += length;
                }
                if (finished) {
                    deviceSession = getDeviceSession(channel, remoteAddress);
                    Position position = new Position(getProtocolName());
                    position.setDeviceId(deviceSession.getDeviceId());
                    getLastLocation(position, null);
                    try {
                        if (video) {
                            position.set(Position.KEY_VIDEO, writeMediaFile(uniqueId, media, "h265"));
                        } else {
                            position.set(Position.KEY_IMAGE, writeMediaFile(uniqueId, media, "jpg"));
                        }
                    } finally {
                        media.release();
                        media = null;
                    }
                    if (channel != null) {
                        ByteBuf response = Unpooled.buffer();
                        response.writeShort(MSG_INIT_REQUEST);
                        channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
                    }
                    return position;
                }
                break;
            case MSG_PATH_RESPONSE:
                String file = buf.readCharSequence(buf.readUnsignedShort(), StandardCharsets.US_ASCII).toString();
                if (channel != null) {
                    ByteBuf response = Unpooled.buffer();
                    response.writeShort(MSG_FILE_REQUEST);
                    response.writeShort(file.length());
                    response.writeCharSequence(file, StandardCharsets.US_ASCII);
                    channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
                }
                break;
            default:
                break;
        }

        return null;
    }

}
