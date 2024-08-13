/*
 * Copyright 2018 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2018 Andrey Kunitsyn (andrey@traccar.org)
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

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public final class BufferUtil {

    private BufferUtil() {
    }

    public static int readSignedMagnitudeInt(ByteBuf buffer) {
        long value = buffer.readUnsignedInt();
        int result = (int) BitUtil.to(value, 31);
        return BitUtil.check(value, 31) ? -result : result;
    }

    public static int indexOf(ByteBuf buffer, int fromIndex, int toIndex, byte value, int count) {
        int startIndex = fromIndex;
        for (int i = 0; i < count; i++) {
            int result = buffer.indexOf(startIndex, toIndex, value);
            if (result < 0 || i == count - 1) {
                return result;
            }
            startIndex = result + 1;
        }
        return -1;
    }

    public static int indexOf(String needle, ByteBuf haystack) {
        return indexOf(needle, haystack, haystack.readerIndex(), haystack.writerIndex());
    }

    public static int indexOf(String needle, ByteBuf haystack, int startIndex, int endIndex) {
        ByteBuf wrappedNeedle = Unpooled.wrappedBuffer(needle.getBytes(StandardCharsets.US_ASCII));
        try {
            return indexOf(wrappedNeedle, haystack, startIndex, endIndex);
        } finally {
            wrappedNeedle.release();
        }
    }

    public static int indexOf(ByteBuf needle, ByteBuf haystack, int startIndex, int endIndex) {
        int originalReaderIndex = haystack.readerIndex();
        int originalWriterIndex = haystack.writerIndex();
        try {
            haystack.readerIndex(startIndex);
            haystack.writerIndex(endIndex);
            return ByteBufUtil.indexOf(needle, haystack);
        } finally {
            haystack.readerIndex(originalReaderIndex);
            haystack.writerIndex(originalWriterIndex);
        }
    }

    public static boolean isPrintable(ByteBuf buf, int length) {
        boolean printable = true;
        for (int i = 0; i < length; i++) {
            byte b = buf.getByte(buf.readerIndex() + i);
            if (b < 32 && b != '\r' && b != '\n') {
                printable = false;
                break;
            }
        }
        return printable;
    }

    public static String readString(ByteBuf buf, int length) {
        return buf.readCharSequence(length, StandardCharsets.US_ASCII).toString();
    }

}
