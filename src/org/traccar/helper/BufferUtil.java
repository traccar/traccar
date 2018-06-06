/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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

public class BufferUtil {

    public static int indexOf(String needle, ByteBuf haystack) {
        return ByteBufUtil.indexOf(
                Unpooled.wrappedBuffer(needle.getBytes(StandardCharsets.US_ASCII)), haystack);
    }

    public static int indexOf(String needle, ByteBuf haystack, int startIndex, int endIndex) {
        int index = ByteBufUtil.indexOf(
                Unpooled.wrappedBuffer(needle.getBytes(StandardCharsets.US_ASCII)),
                Unpooled.wrappedBuffer(haystack.array(), startIndex, endIndex - startIndex));
        return (index != -1) ? (startIndex + index) : -1;
    }

}
