/*
 * Copyright 2015 - 2018 Anton Tananaev (anton@traccar.org)
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
package org.traccar;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

public class CharacterDelimiterFrameDecoder extends DelimiterBasedFrameDecoder {

    private static ByteBuf createDelimiter(char delimiter) {
        byte[] buf = {(byte) delimiter};
        return Unpooled.wrappedBuffer(buf);
    }

    private static ByteBuf createDelimiter(String delimiter) {
        byte[] buf = new byte[delimiter.length()];
        for (int i = 0; i < delimiter.length(); i++) {
            buf[i] = (byte) delimiter.charAt(i);
        }
        return Unpooled.wrappedBuffer(buf);
    }

    private static ByteBuf[] convertDelimiters(String[] delimiters) {
        ByteBuf[] result = new ByteBuf[delimiters.length];
        for (int i = 0; i < delimiters.length; i++) {
            result[i] = createDelimiter(delimiters[i]);
        }
        return result;
    }

    public CharacterDelimiterFrameDecoder(int maxFrameLength, char delimiter) {
        super(maxFrameLength, createDelimiter(delimiter));
    }

    public CharacterDelimiterFrameDecoder(int maxFrameLength, String delimiter) {
        super(maxFrameLength, createDelimiter(delimiter));
    }

    public CharacterDelimiterFrameDecoder(int maxFrameLength, boolean stripDelimiter, String delimiter) {
        super(maxFrameLength, stripDelimiter, createDelimiter(delimiter));
    }

    public CharacterDelimiterFrameDecoder(int maxFrameLength, String... delimiters) {
        super(maxFrameLength, convertDelimiters(delimiters));
    }

    public CharacterDelimiterFrameDecoder(int maxFrameLength, boolean stripDelimiter, String... delimiters) {
        super(maxFrameLength, stripDelimiter, convertDelimiters(delimiters));
    }

}
