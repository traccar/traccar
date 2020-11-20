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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public final class Checksum {

    private Checksum() {
    }

    public static class Algorithm {

        private final int poly;
        private final int init;
        private final boolean refIn;
        private final boolean refOut;
        private final int xorOut;
        private final int[] table;

        public Algorithm(int bits, int poly, int init, boolean refIn, boolean refOut, int xorOut) {
            this.poly = poly;
            this.init = init;
            this.refIn = refIn;
            this.refOut = refOut;
            this.xorOut = xorOut;
            this.table = bits == 8 ? initTable8() : initTable16();
        }

        private int[] initTable8() {
            int[] table = new int[256];
            int crc;
            for (int i = 0; i < 256; i++) {
                crc = i;
                for (int j = 0; j < 8; j++) {
                    boolean bit = (crc & 0x80) != 0;
                    crc <<= 1;
                    if (bit) {
                        crc ^= poly;
                    }
                }
                table[i] = crc & 0xFF;
            }
            return table;
        }

        private int[] initTable16() {
            int[] table = new int[256];
            int crc;
            for (int i = 0; i < 256; i++) {
                crc = i << 8;
                for (int j = 0; j < 8; j++) {
                    boolean bit = (crc & 0x8000) != 0;
                    crc <<= 1;
                    if (bit) {
                        crc ^= poly;
                    }
                }
                table[i] = crc & 0xFFFF;
            }
            return table;
        }

    }

    private static int reverse(int value, int bits) {
        int result = 0;
        for (int i = 0; i < bits; i++) {
            result = (result << 1) | (value & 1);
            value >>= 1;
        }
        return result;
    }

    public static int crc8(Algorithm algorithm, ByteBuffer buf) {
        int crc = algorithm.init;
        while (buf.hasRemaining()) {
            int b = buf.get() & 0xFF;
            if (algorithm.refIn) {
                b = reverse(b, 8);
            }
            crc = algorithm.table[(crc & 0xFF) ^ b];
        }
        if (algorithm.refOut) {
            crc = reverse(crc, 8);
        }
        return (crc ^ algorithm.xorOut) & 0xFF;
    }

    public static int crc16(Algorithm algorithm, ByteBuffer buf) {
        int crc = algorithm.init;
        while (buf.hasRemaining()) {
            int b = buf.get() & 0xFF;
            if (algorithm.refIn) {
                b = reverse(b, 8);
            }
            crc = (crc << 8) ^ algorithm.table[((crc >> 8) & 0xFF) ^ b];
        }
        if (algorithm.refOut) {
            crc = reverse(crc, 16);
        }
        return (crc ^ algorithm.xorOut) & 0xFFFF;
    }

    public static final Algorithm CRC8_EGTS = new Algorithm(8, 0x31, 0xFF, false, false, 0x00);
    public static final Algorithm CRC8_ROHC = new Algorithm(8, 0x07, 0xFF, true, true, 0x00);

    public static final Algorithm CRC16_IBM = new Algorithm(16, 0x8005, 0x0000, true, true, 0x0000);
    public static final Algorithm CRC16_X25 = new Algorithm(16, 0x1021, 0xFFFF, true, true, 0xFFFF);
    public static final Algorithm CRC16_MODBUS = new Algorithm(16, 0x8005, 0xFFFF, true, true, 0x0000);
    public static final Algorithm CRC16_CCITT_FALSE = new Algorithm(16, 0x1021, 0xFFFF, false, false, 0x0000);
    public static final Algorithm CRC16_KERMIT = new Algorithm(16, 0x1021, 0x0000, true, true, 0x0000);
    public static final Algorithm CRC16_XMODEM = new Algorithm(16, 0x1021, 0x0000, false, false, 0x0000);

    public static int crc32(ByteBuffer buf) {
        CRC32 checksum = new CRC32();
        while (buf.hasRemaining()) {
            checksum.update(buf.get());
        }
        return (int) checksum.getValue();
    }

    public static int xor(ByteBuffer buf) {
        int checksum = 0;
        while (buf.hasRemaining()) {
            checksum ^= buf.get();
        }
        return checksum;
    }

    public static int xor(String string) {
        byte checksum = 0;
        for (byte b : string.getBytes(StandardCharsets.US_ASCII)) {
            checksum ^= b;
        }
        return checksum;
    }

    public static String nmea(String string) {
        return String.format("*%02X", xor(string));
    }

    public static int sum(ByteBuffer buf) {
        byte checksum = 0;
        while (buf.hasRemaining()) {
            checksum += buf.get();
        }
        return checksum;
    }

    public static int modulo256(ByteBuffer buf) {
        int checksum = 0;
        while (buf.hasRemaining()) {
            checksum = (checksum + buf.get()) & 0xFF;
        }
        return checksum;
    }

    public static String sum(String msg) {
        byte checksum = 0;
        for (byte b : msg.getBytes(StandardCharsets.US_ASCII)) {
            checksum += b;
        }
        return String.format("%02X", checksum).toUpperCase();
    }

    public static long luhn(long imei) {
        long checksum = 0;
        long remain = imei;

        for (int i = 0; remain != 0; i++) {
            long digit = remain % 10;

            if (i % 2 == 0) {
                digit *= 2;
                if (digit >= 10) {
                    digit = 1 + (digit % 10);
                }
            }

            checksum += digit;
            remain /= 10;
        }

        return (10 - (checksum % 10)) % 10;
    }

}
