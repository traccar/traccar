/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.traccar.BaseProtocolEncoder;
import org.traccar.helper.Crc;
import org.traccar.model.Command;

public class Gt06ProtocolEncoder extends BaseProtocolEncoder {
    
    @Override
    protected Object encodeCommand(Command command) {
        
        String content = "";
        switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP:
                content = "DYD#";
                break;
            case Command.TYPE_ENGINE_RESUME:
                content = "HFYD#";
                break;
        }
        
        int serverFlagBit = 0x00;
        int commandLength = serverFlagBit + content.length();
        int packetLength =  0x80 + content.length() + 2 + 2;

        ChannelBuffer response = ChannelBuffers.directBuffer(10);
        response.writeBytes(new byte[]{0x78, 0x78}); // Start Bit
        response.writeByte(packetLength); // Packet Length
        response.writeByte(0x80); // Protocol Number

        // Information Content
        response.writeByte(commandLength); // Length of command
        response.writeByte(serverFlagBit); // Server Flag Bit
        response.writeBytes(content.getBytes()); // Command Content
        response.writeBytes(new byte[]{0x00, 0x02}); // Language

        response.writeShort(1); // Information Serial Number

        int crc = Crc.crc16Ccitt(response.toByteBuffer(2, response.writerIndex()));
        response.writeShort(crc); // Error Check

        response.writeBytes(new byte[] {0x0D, 0x0A}); // Stop Bit

        return response;
    }
    
}
