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
        ByteBuf wrappedHaystack;
        if (startIndex == haystack.readerIndex() && endIndex == haystack.writerIndex()) {
            wrappedHaystack = haystack;
        } else {
            wrappedHaystack = Unpooled.wrappedBuffer(haystack);
            wrappedHaystack.readerIndex(startIndex - haystack.readerIndex());
            wrappedHaystack.writerIndex(endIndex - haystack.readerIndex());
        }
        int result = ByteBufUtil.indexOf(needle, wrappedHaystack);
        return result < 0 ? result : startIndex + result;
    }

}
