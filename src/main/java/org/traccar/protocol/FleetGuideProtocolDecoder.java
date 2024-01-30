/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.session.DeviceSession;

import java.lang.reflect.Method;
import java.net.SocketAddress;

public class FleetGuideProtocolDecoder extends BaseProtocolDecoder {

    public FleetGuideProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_DATA = 0x10;
    public static final int MSG_HEARTBEAT = 0x1A;
    public static final int MSG_RESPONSE = 0x1C;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // signature
        int options = buf.readUnsignedShortLE();
        int length = BitUtil.to(options, 11);

        DeviceSession deviceSession;
        if (BitUtil.check(options, 11)) {
            deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(buf.readUnsignedIntLE()));
        } else {
            deviceSession = getDeviceSession(channel, remoteAddress);
        }
        if (deviceSession == null) {
            return null;
        }

        int type;
        if (BitUtil.check(options, 12)) {
            type = BitUtil.to(buf.readUnsignedByte(), 4);
        } else {
            type = 0;
        }

        if (BitUtil.check(options, 13)) {
            buf.readUnsignedShortLE(); // acknowledgement
        }

        ByteBuf data;
        if (BitUtil.check(options, 14)) {
            data = decompress(buf.readSlice(length));
        } else {
            data = buf.readRetainedSlice(length);
        }

        data.release();

        return null;
    }

    private ByteBuf decompress(ByteBuf input) throws ReflectiveOperationException {

        Class<?> clazz = Class.forName("io.netty.handler.codec.compression.FastLz");
        Method method = clazz.getDeclaredMethod(
                "decompress", ByteBuf.class, int.class, int.class, ByteBuf.class, int.class, int.class);
        method.setAccessible(true);

        ByteBuf output = Unpooled.buffer();
        int result = (Integer) method.invoke(
                null, input, input.readerIndex(), input.readableBytes(), output, 0, Integer.MAX_VALUE);
        if (result <= 0) {
            throw new IllegalArgumentException("FastLz decompression failed");
        }

        return output;
    }

}
