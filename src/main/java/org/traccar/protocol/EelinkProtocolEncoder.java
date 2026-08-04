/*
 * Copyright 2016 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.session.ConnectionManager;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;
import org.traccar.Protocol;
import org.traccar.session.DeviceSession;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class EelinkProtocolEncoder extends BaseProtocolEncoder {

    private boolean connectionless;
    private ConnectionManager connectionManager;

    public EelinkProtocolEncoder(Protocol protocol, boolean connectionless) {
        super(protocol);
        this.connectionless = connectionless;
    }

    @jakarta.inject.Inject
    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public static int checksum(ByteBuffer buf) {
        int sum = 0;
        while (buf.hasRemaining()) {
            sum = (((sum << 1) | (sum >> 15)) + (buf.get() & 0xFF)) & 0xFFFF;
        }
        return sum;
    }

    public static ByteBuf encodeContent(
            boolean connectionless, String uniqueId, int type, int index, ByteBuf content) {

        ByteBuf buf = Unpooled.buffer();

        if (connectionless) {
            buf.writeBytes(DataConverter.parseHex('0' + uniqueId));
        }

        buf.writeByte(0x67);
        buf.writeByte(0x67);
        buf.writeByte(type);
        buf.writeShort(2 + (content != null ? content.readableBytes() : 0)); // length
        buf.writeShort(index);

        if (content != null) {
            buf.writeBytes(content);
        }

        ByteBuf result = Unpooled.buffer();

        if (connectionless) {
            result.writeByte('E');
            result.writeByte('L');
            result.writeShort(2 + buf.readableBytes()); // length
            result.writeShort(checksum(buf.nioBuffer()));
        }

        result.writeBytes(buf);
        buf.release();

        return result;
    }

    private ByteBuf encodeContent(long deviceId, String content) {

        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(0x01); // command
        buf.writeInt(0); // server id
        buf.writeBytes(content.getBytes(StandardCharsets.UTF_8));

        return encodeContent(connectionless, getUniqueId(deviceId), EelinkProtocolDecoder.MSG_DOWNLINK, 0, buf);
    }

    private boolean isItr120Device(long deviceId) {
        if (connectionManager != null) {
            DeviceSession deviceSession = connectionManager.getDeviceSession(deviceId);
            if (deviceSession != null && deviceSession.contains(EelinkProtocolDecoder.KEY_ITR120_SESSION)) {
                return true;
            }
        }
        String model = getDeviceModel(deviceId);
        return model != null && model.toUpperCase(Locale.ROOT).contains("ITR120");
    }

    private ByteBuf encodeItr120Command(long sequence, String content) {
        byte[] data = content.getBytes(StandardCharsets.US_ASCII);
        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(0x28);
        buf.writeByte(0x28);
        buf.writeByte(EelinkProtocolDecoder.MSG_DOWNLINK);
        buf.writeShort(2 + 1 + 4 + data.length);
        buf.writeShort((int) sequence);
        buf.writeByte(0x01);
        buf.writeInt((int) sequence);
        buf.writeBytes(data);

        return buf;
    }

    private String getItr120Content(Command command) {
        return switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP -> "RELAY,1#";
            case Command.TYPE_ENGINE_RESUME -> "RELAY,0#";
            case Command.TYPE_CUSTOM -> command.getString(Command.KEY_DATA);
            default -> null;
        };
    }

    private String getClassicContent(Command command) {
        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> command.getString(Command.KEY_DATA);
            case Command.TYPE_POSITION_SINGLE -> "WHERE#";
            case Command.TYPE_ENGINE_STOP -> "RELAY,1#";
            case Command.TYPE_ENGINE_RESUME -> "RELAY,0#";
            case Command.TYPE_REBOOT_DEVICE -> "RESET#";
            default -> null;
        };
    }

    @Override
    protected Object encodeCommand(Command command) {
        boolean itr120 = isItr120Device(command.getDeviceId());
        String content = itr120 ? getItr120Content(command) : getClassicContent(command);
        if (content == null) {
            return null;
        }
        if (itr120) {
            long sequence = command.getDeviceId() & 0xffffL;
            return encodeItr120Command(sequence != 0 ? sequence : 1, content);
        }
        return encodeContent(command.getDeviceId(), content);
    }

}
