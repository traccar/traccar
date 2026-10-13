/*
 * Copyright 2015 - 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.CharsetUtil;
import org.traccar.helper.Checksum;
import org.traccar.model.Command;

public class KhdProtocolEncoder extends BaseProtocolEncoder {

    public static final int MSG_ON_DEMAND_TRACK = 0x30;
    public static final int MSG_REBOOT = 0x32;
    public static final int MSG_CUT_OIL = 0x39;
    public static final int MSG_SEND_TEXT = 0x3A;
    public static final int MSG_RESUME_OIL = 0x38;
    public static final int MSG_CHECK_VERSION = 0x3D;
    public static final int MSG_FACTORY_RESET = 0xC3;
    public static final int MSG_SET_OVERSPEED = 0x3F;
    public static final int MSG_DELETE_MILEAGE = 0x66;
    public static final int MSG_PERIODIC_TRACK = 0x7B;

    public KhdProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeCommand(int command, String uniqueId, ByteBuf content) {

        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(0x29);
        buf.writeByte(0x29);

        buf.writeByte(command);

        int length = 6;
        if (content != null) {
            length += content.readableBytes();
        }
        buf.writeShort(length);

        buf.writeBytes(decodeUniqueId(uniqueId));

        if (content != null) {
            buf.writeBytes(content);
        }

        buf.writeByte(Checksum.xor(buf.nioBuffer()));
        buf.writeByte(0x0D); // ending

        return buf;
    }

    /**
     * Forward S18 mask encoding: 11-digit SIM phone -> 4-byte Fake IP.
     * Inverse of KhdProtocolDecoder.decodeFakeIp. Returns null when the
     * string is not an encodable SIM (wrong length/prefix), so the caller
     * falls through to the legacy or hex paths.
     */
    static byte[] encodeSimAsFakeIp(String uniqueId) {
        if (uniqueId == null || uniqueId.length() != 11 || uniqueId.charAt(0) != '1') {
            return null;
        }
        for (int i = 0; i < 11; i++) {
            if (!Character.isDigit(uniqueId.charAt(i))) {
                return null;
            }
        }
        int n = Integer.parseInt(uniqueId.substring(1, 3));
        int mask = n - 30 < 10 ? n - 30 : n - 30 - 16;
        if (mask < 0 || mask > 15) {
            int extended = n - 30; // non-standard 144/145 prefixes, masks 14/15
            if (extended < 14 || extended > 15) {
                return null;
            }
            mask = extended;
        }
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            int pair = Integer.parseInt(uniqueId.substring(3 + 2 * i, 5 + 2 * i));
            if ((mask & (0x08 >> i)) != 0) {
                pair += 0x80;
            }
            result[i] = (byte) pair;
        }
        return result;
    }

    private byte[] decodeUniqueId(String uniqueId) {
        byte[] simFakeIp = encodeSimAsFakeIp(uniqueId);
        if (simFakeIp != null) {
            return simFakeIp;
        }
        if (uniqueId != null && uniqueId.matches("\\d{9,10}")) {
            // S18 Fake IP registered as unsigned 32-bit decimal, e.g. 3369902872.
            long value = Long.parseLong(uniqueId);
            if (value <= 0xFFFFFFFFL) {
                return new byte[] {
                    (byte) (value >> 24), (byte) (value >> 16),
                    (byte) (value >> 8), (byte) value};
            }
        }
        try {
            String padded = "00000000".concat(uniqueId);
            padded = padded.substring(padded.length() - 8);
            return new byte[] {
                (byte) Integer.parseInt(padded.substring(0, 2)),
                (byte) (Integer.parseInt(padded.substring(2, 4)) + 0x80),
                (byte) (Integer.parseInt(padded.substring(4, 6)) + 0x80),
                (byte) Integer.parseInt(padded.substring(6, 8))};
        } catch (NumberFormatException e) {
            // S18 Fake-IP hex identifier, e.g. 0A9B6584: raw bytes, no KHD bias.
            String hex = uniqueId.replaceAll("[^0-9A-Fa-f]", "");
            hex = "00000000".concat(hex);
            hex = hex.substring(hex.length() - 8);
            byte[] result = new byte[4];
            for (int i = 0; i < 4; i++) {
                result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            return result;
        }
    }

    @Override
    protected Object encodeCommand(Command command) {

        String uniqueId = getUniqueId(command.getDeviceId());

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> {
                ByteBuf content = Unpooled.copiedBuffer(
                        command.getString(Command.KEY_DATA), CharsetUtil.GB2312);
                yield encodeCommand(MSG_SEND_TEXT, uniqueId, content);
            }
            case Command.TYPE_ENGINE_STOP -> encodeCommand(MSG_CUT_OIL, uniqueId, null);
            case Command.TYPE_ENGINE_RESUME -> encodeCommand(MSG_RESUME_OIL, uniqueId, null);
            case Command.TYPE_GET_VERSION -> encodeCommand(MSG_CHECK_VERSION, uniqueId, null);
            case Command.TYPE_FACTORY_RESET -> encodeCommand(MSG_FACTORY_RESET, uniqueId, null);
            case Command.TYPE_SET_SPEED_LIMIT -> {
                ByteBuf content = Unpooled.buffer();
                content.writeByte(command.getInteger(Command.KEY_DATA));
                yield encodeCommand(MSG_SET_OVERSPEED, uniqueId, content);
            }
            case Command.TYPE_SET_ODOMETER -> encodeCommand(MSG_DELETE_MILEAGE, uniqueId, null);
            case Command.TYPE_POSITION_SINGLE -> encodeCommand(MSG_ON_DEMAND_TRACK, uniqueId, null);
            case Command.TYPE_POSITION_PERIODIC -> {
                ByteBuf content = Unpooled.buffer();
                content.writeShort(command.getInteger(Command.KEY_FREQUENCY));
                content.writeShort(0xFFFF); // report count
                yield encodeCommand(MSG_PERIODIC_TRACK, uniqueId, content);
            }
            case Command.TYPE_REBOOT_DEVICE -> encodeCommand(MSG_REBOOT, uniqueId, null);
            default -> null;
        };
    }

}
