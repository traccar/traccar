/*
 * Copyright 2017 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.model.Command;

public class GatorProtocolEncoder extends BaseProtocolEncoder {

    public GatorProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeCommand(String pseudoAddress, int commandType) {
        ByteBuf buf = Unpooled.buffer();

        buf.writeShort(0x2424);

        buf.writeByte(commandType);

        buf.writeShort(0x0006); // Length

        int[] ipAddress = idToPseudoIPAddress(pseudoAddress);

        buf.writeByte(ipAddress[0]);
        buf.writeByte(ipAddress[1]);
        buf.writeByte(ipAddress[2]);
        buf.writeByte(ipAddress[3]);

        buf.writeByte(Checksum.xor(buf.nioBuffer(2, buf.writerIndex())));

        buf.writeByte(0x0D);

        return buf;
    }

    public int[] idToPseudoIPAddress(String deviceID) {
        if (deviceID.length() != 11) {
            return new int[4];
        }

        int[] ipAddress = new int[4];
        for (int i = 0; i < 4; i++) {
            ipAddress[i] = Integer.parseInt(deviceID.substring(3 + i * 2, 5 + i * 2));
        }

        ipAddress[0] = ipAddress[0] % 0x7f;
        ipAddress[1] = ipAddress[1] % 0x7f;
        ipAddress[2] = ipAddress[2] % 0x7f;
        ipAddress[3] = ipAddress[3] % 0x7f;

        return ipAddress;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_ENGINE_RESUME:
                return encodeCommand(getUniqueId(command.getDeviceId()), GatorProtocolDecoder.MSG_ENGINE_ENABLE);
            case Command.TYPE_ENGINE_STOP:
                return encodeCommand(getUniqueId(command.getDeviceId()), GatorProtocolDecoder.MSG_ENGINE_STOP);
            case Command.TYPE_POSITION_SINGLE:
                return encodeCommand(getUniqueId(command.getDeviceId()), GatorProtocolDecoder.MSG_LOCATE_IMMEDIATE);
            default:
                return null;
        }
    }

}
