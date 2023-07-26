/*
 * Copyright 2023 Hossain Mohammad Seym (seym45@gmail.com)
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

import java.util.ArrayList;
import java.util.List;

public class GatorProtocolEncoder extends BaseProtocolEncoder {

    public GatorProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    public static ByteBuf encodeId(long deviceId) {
        ByteBuf buf = Unpooled.buffer();

        String deviceIdStrVal = String.valueOf(deviceId);
        List<String> partialDigits = new ArrayList<>();
        for (int i = 1; i < deviceIdStrVal.length(); i += 2) {
            partialDigits.add(deviceIdStrVal.substring(i, i + 2));
        }

        int firstDigit = Integer.parseInt(partialDigits.get(0)) - 30;

        for (int i = 1; i < partialDigits.size(); i++) {
            int shiftCount = 4 - i;
            int addend = ((firstDigit & (1 << shiftCount)) >> shiftCount) << 7;
            int sum = Integer.parseInt(partialDigits.get(i)) | addend;

            buf.writeByte(sum);
        }

        return buf;
    }

    private ByteBuf encodeContent(long deviceId, int mainOrder) {
        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(0x24);
        buf.writeByte(0x24);
        buf.writeByte(mainOrder);
        buf.writeByte(0x00);
        buf.writeByte(4 + 1 + 1); // ip 4 bytes, checksum and end byte

        ByteBuf pseudoIPAddress = encodeId(deviceId);
        buf.writeBytes(pseudoIPAddress);

        int checksum = Checksum.xor(buf.nioBuffer());
        buf.writeByte(checksum);

        buf.writeByte(0x0D);

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_POSITION_SINGLE:
                return encodeContent(command.getDeviceId(), GatorProtocolDecoder.MSG_POSITION_REQUEST);
            default:
                return null;
        }
    }
}
