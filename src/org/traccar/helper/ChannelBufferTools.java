/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.util.Formatter;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * ChannelBuffer helper methods
 */
public class ChannelBufferTools {
    
    /**
     * Find string in network buffer
     */
    public static Integer find(
            ChannelBuffer buf,
            Integer start,
            Integer length,
            String subString) {

        int index = start;
        boolean match;

        for (; index < length; index++) {
            match = true;

            for (int i = 0; i < subString.length(); i++) {
                char c = (char) buf.getByte(index + i);
                if (c != subString.charAt(i)) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return index;
            }
        }

        return null;
    }
    
    /**
     * Convert hex to integer (length in hex digits)
     */
    public static int readHexInteger(ChannelBuffer buf, int length) {
        
        int result = 0;
        
        for (int i = 0; i < length / 2; i++) {
            int b = buf.readUnsignedByte();
            result *= 10;
            result += b >>> 4;
            result *= 10;
            result += b & 0x0f;
        }
        
        if (length % 2 == 1) {
            int b = buf.getUnsignedByte(buf.readerIndex());
            result *= 10;
            result += b >>> 4;
        }
        
        return result;
    }

    /**
     * Return hex string
     */
    public static String readHexString(ChannelBuffer buf, int length) {
        
        StringBuilder result = new StringBuilder();
        Formatter formatter = new Formatter(result);
        
        for (int i = 0; i < length / 2; i++) {
            formatter.format("%02x", buf.readByte());
        }
        
        if (length % 2 == 1) {
            int b = buf.getUnsignedByte(buf.readerIndex());
            formatter.format("%01x", b >>> 4);
        }
        
        return result.toString();
    }

    /**
     * Convert integer array to byte array
     */
    public static byte[] convertArray(int[] in) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (byte) in[i];
        }
        return out;
    }
}
