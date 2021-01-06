/*
 * Copyright 2020 - 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.UnitsConverter;
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

            MqttSubscribeMessage message = (MqttSubscribeMessage) msg;

            MqttMessage response = MqttMessageBuilders.subAck()
                    .packetId((short) message.variableHeader().messageId())
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
                ByteBuf record = buf.readSlice(length);
                if (type == 1) {

                    Position position = new Position(getProtocolName());
                    position.setDeviceId(deviceSession.getDeviceId());
                    position.setTime(new Date(record.readUnsignedIntLE() * 1000));

                    while (record.readableBytes() > 0) {
                        int sensorType = record.readUnsignedByte();
                        int sensorId = record.readUnsignedShortLE();
                        if (sensorType == 14) {

                            position.setValid(true);
                            position.setLatitude(record.readFloatLE());
                            position.setLongitude(record.readFloatLE());
                            position.setSpeed(UnitsConverter.knotsFromKph(record.readUnsignedShortLE()));

                            position.set(Position.KEY_HDOP, record.readUnsignedByte());
                            position.set(Position.KEY_SATELLITES, record.readUnsignedByte());

                            position.setCourse(record.readUnsignedShortLE());
                            position.setAltitude(record.readShortLE());

                        } else {

                            if (sensorType == 3) {
                                continue;
                            }

                            String key;
                            switch (sensorId) {
                                case 0x0008:
                                    if (sensorType > 0) {
                                        position.set(Position.KEY_ALARM, Position.ALARM_JAMMING);
                                    }
                                    break;
                                case 0x0010:
                                case 0x0011:
                                case 0x0012:
                                case 0x0013:
                                    key = Position.PREFIX_IN + (sensorId - 0x0010 + 2);
                                    position.set(key, sensorType > 0);
                                    break;
                                case 0x001E:
                                    position.set("buttonPresent", sensorType > 0);
                                    break;
                                case 0x006D:
                                    position.set(Position.KEY_IGNITION, sensorType > 0);
                                    break;
                                case 0x3000:
                                    position.set(Position.KEY_POWER, record.readUnsignedShortLE() * 0.001);
                                    break;
                                case 0x3001:
                                case 0x3002:
                                case 0x3003:
                                    key = Position.PREFIX_ADC + (0x3003 - sensorId + 3);
                                    position.set(key, record.readUnsignedShortLE() * 0.001);
                                    break;
                                case 0x3004:
                                    position.set(Position.KEY_BATTERY, record.readUnsignedShortLE() * 0.001);
                                    break;
                                case 0x300C:
                                    position.set(Position.KEY_RPM, record.readUnsignedShortLE());
                                    break;
                                case 0x4003:
                                    position.set(Position.KEY_ODOMETER, record.readUnsignedIntLE() * 5);
                                    break;
                                case 0x5000:
                                    position.set(Position.KEY_DRIVER_UNIQUE_ID, String.valueOf(record.readLongLE()));
                                    break;
                                case 0xA001:
                                    position.set(Position.KEY_ACCELERATION, record.readFloatLE());
                                    break;
                                case 0xA002:
                                    position.set("cornering", record.readFloatLE());
                                    break;
                                case 0xA017:
                                    key = Position.PREFIX_TEMP + (sensorId - 0xA017 + 1);
                                    position.set(key, record.readFloatLE());
                                    break;
                                default:
                                    key = Position.PREFIX_IO + sensorId;
                                    position.getAttributes().put(key, readValue(record, sensorType));
                                    break;
                            }

                        }
                    }

                    positions.add(position);

                } else if (type == 3) {

                    Position position = new Position(getProtocolName());
                    position.setDeviceId(deviceSession.getDeviceId());

                    getLastLocation(position, new Date(record.readUnsignedIntLE() * 1000));

                    record.readUnsignedByte(); // function identifier

                    position.set(Position.KEY_EVENT, record.readUnsignedByte());

                    positions.add(position);

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
