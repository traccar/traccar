/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class IotmProtocolDecoder extends BaseProtocolDecoder {

    public IotmProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private Object readValue(ByteBuf buf, int sensorType) {
        switch (sensorType) {
            case 0:
                return false;
            case 1:
                return true;
            case 3:
                return 0;
            case 4:
                return buf.readUnsignedByte();
            case 5:
                return buf.readUnsignedShortLE();
            case 6:
                return buf.readUnsignedIntLE();
            case 7:
            case 11:
                return buf.readLongLE();
            case 8:
                return buf.readByte();
            case 9:
                return buf.readShortLE();
            case 10:
                return buf.readIntLE();
            case 12:
                return buf.readFloatLE();
            case 13:
                return buf.readDoubleLE();
            case 32:
                return buf.readCharSequence(buf.readUnsignedByte(), StandardCharsets.US_ASCII).toString();
            case 33:
                return ByteBufUtil.hexDump(buf.readSlice(buf.readUnsignedByte()));
            case 64:
                return buf.readCharSequence(buf.readUnsignedShortLE(), StandardCharsets.US_ASCII).toString();
            case 65:
                return ByteBufUtil.hexDump(buf.readSlice(buf.readUnsignedShortLE()));
            case 2:
            default:
                return null;
        }
    }

    private String getKey(int sensorId) {
        switch (sensorId) {
            case 0x300C:
                return Position.KEY_RPM;
            case 0x4003:
                return Position.KEY_ODOMETER;
            default:
                return null;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        if (msg instanceof MqttConnectMessage) {

            MqttConnectMessage message = (MqttConnectMessage) msg;

            DeviceSession deviceSession = getDeviceSession(
                    channel, remoteAddress, message.payload().clientIdentifier());

            MqttConnectReturnCode returnCode = deviceSession != null
                    ? MqttConnectReturnCode.CONNECTION_ACCEPTED
                    : MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;

            MqttMessage response = MqttMessageBuilders.connAck().returnCode(returnCode).build();

            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            }

        } else if (msg instanceof MqttSubscribeMessage) {

            MqttPublishMessage message = (MqttPublishMessage) msg;

            MqttMessage response = MqttMessageBuilders.subAck()
                    .packetId((short) message.variableHeader().packetId())
                    .build();

            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            }

        } else if (msg instanceof MqttPublishMessage) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            List<Position> positions = new LinkedList<>();

            MqttPublishMessage message = (MqttPublishMessage) msg;
            ByteBuf buf = message.payload();

            buf.readUnsignedByte(); // structure version

            while (buf.readableBytes() > 1) {
                int type = buf.readUnsignedByte();
                int length = buf.readUnsignedShortLE();
                if (type == 1) {

                    ByteBuf record = buf.readSlice(length);

                    Position position = new Position(getProtocolName());
                    position.setDeviceId(deviceSession.getDeviceId());
                    position.setTime(new Date(record.readUnsignedIntLE()));

                    while (record.readableBytes() > 0) {
                        int sensorType = record.readUnsignedByte();
                        int sensorId = record.readUnsignedShortLE();
                        if (sensorType == 14) {

                            position.setValid(true);
                            position.setLatitude(record.readFloatLE());
                            position.setLongitude(record.readFloatLE());
                            position.setSpeed(record.readUnsignedShortLE());

                            position.set(Position.KEY_HDOP, record.readUnsignedByte());
                            position.set(Position.KEY_SATELLITES, record.readUnsignedByte());

                            position.setCourse(record.readUnsignedShortLE());
                            position.setAltitude(record.readShortLE());

                        } else {

                            String key = getKey(sensorId);
                            Object value = readValue(record, sensorType);
                            if (key != null && value != null) {
                                position.getAttributes().put(key, value);
                            }

                        }
                    }

                    positions.add(position);

                } else {
                    buf.skipBytes(length);
                }
            }

            buf.readUnsignedByte(); // checksum

            MqttMessage response = MqttMessageBuilders.pubAck()
                    .packetId((short) message.variableHeader().packetId())
                    .build();

            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            }

            return positions.isEmpty() ? null : positions;

        }

        return null;
    }

}
