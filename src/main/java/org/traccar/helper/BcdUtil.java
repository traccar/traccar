/*
 * Copyright 2012 - 2018 Anton Tananaev (anton@traccar.org)
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
package org.traccar.helper;

import io.netty.buffer.ByteBuf;

public final class BcdUtil {

    private BcdUtil() {
    }

    public static int readInteger(ByteBuf buf, int digits) {
        int result = 0;

        for (int i = 0; i < digits / 2; i++) {
            int b = buf.readUnsignedByte();
            result *= 10;
            result += b >>> 4;
            result *= 10;
            result += b & 0x0f;
        }

        if (digits % 2 != 0) {
            int b = buf.getUnsignedByte(buf.readerIndex());
            result *= 10;
            result += b >>> 4;
        }

        return result;
    }

    public static double readCoordinate(ByteBuf buf) {
        int b1 = buf.readUnsignedByte();
        int b2 = buf.readUnsignedByte();
        int b3 = buf.readUnsignedByte();
        int b4 = buf.readUnsignedByte();

        double value = (b2 & 0xf) * 10 + (b3 >> 4);
        value += (((b3 & 0xf) * 10 + (b4 >> 4)) * 10 + (b4 & 0xf)) / 1000.0;
        value /= 60;
        value += ((b1 >> 4 & 0x7) * 10 + (b1 & 0xf)) * 10 + (b2 >> 4);

        if ((b1 & 0x80) != 0) {
            value = -value;
        }

        return value;
    }

}
