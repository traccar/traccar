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
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;

import java.net.SocketAddress;

public class IotmProtocolDecoder extends BaseProtocolDecoder {

    public IotmProtocolDecoder(Protocol protocol) {
        super(protocol);
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

            MqttConnAckMessage response = MqttMessageBuilders.connAck().returnCode(returnCode).build();

            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));

        } else if (msg instanceof MqttPublishMessage) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            MqttPublishMessage message = (MqttPublishMessage) msg;
            ByteBuf bug = message.payload();

            return null;

        }

        return null;
    }

}
