/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
package org.traccar.media;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.traccar.helper.Checksum;

public class VideoStreamWriter {

    private static final int TS_PACKET_SIZE = 188;
    private static final int PMT_PID = 0x1000;
    private static final int VIDEO_PID = 0x0100;
    private static final int STREAM_TYPE_H264 = 0x1B;

    private int patContinuityCounter;
    private int pmtContinuityCounter;
    private int videoContinuityCounter;

    public void write(ByteBuf output, ByteBuf nalData, long pts, boolean isKeyFrame) {
        if (isKeyFrame) {
            writePat(output);
            writePmt(output);
        }

        long pts90k = pts * 90;
        ByteBuf pesPacket = createPes(nalData, pts90k);
        writePesPackets(output, pesPacket, isKeyFrame, pts90k);
        pesPacket.release();
    }

    private void writePat(ByteBuf output) {
        int start = output.writerIndex();

        // TS header
        output.writeByte(0x47); // sync byte
        output.writeByte(0x40); // payload unit start + PID high (0)
        output.writeByte(0x00); // PID low (PAT = 0x0000)
        output.writeByte(0x10 | (patContinuityCounter++ & 0x0F)); // payload only

        // pointer field
        output.writeByte(0x00);

        // PAT table
        int tableStart = output.writerIndex();
        output.writeByte(0x00); // table id
        output.writeShort(0xB00D); // section syntax indicator + section length (13)
        output.writeShort(0x0001); // transport stream id
        output.writeByte(0xC1); // reserved + version 0 + current
        output.writeByte(0x00); // section number
        output.writeByte(0x00); // last section number
        output.writeShort(0x0001); // program number
        output.writeShort(0xE000 | PMT_PID); // reserved + PMT PID

        // CRC32
        output.writeInt(Checksum.crc32(
                Checksum.CRC32_MPEG2, output.nioBuffer(tableStart, output.writerIndex() - tableStart)));

        // fill rest with 0xFF
        int remaining = TS_PACKET_SIZE - (output.writerIndex() - start);
        for (int i = 0; i < remaining; i++) {
            output.writeByte(0xFF);
        }
    }

    private void writePmt(ByteBuf output) {
        int start = output.writerIndex();

        // TS header
        output.writeByte(0x47);
        output.writeShort(0x4000 | PMT_PID); // payload unit start + PMT PID
        output.writeByte(0x10 | (pmtContinuityCounter++ & 0x0F));

        // pointer field
        output.writeByte(0x00);

        // PMT table
        int tableStart = output.writerIndex();
        output.writeByte(0x02); // table id
        output.writeShort(0xB012); // section syntax indicator + section length (18)
        output.writeShort(0x0001); // program number
        output.writeByte(0xC1); // reserved + version 0 + current
        output.writeByte(0x00); // section number
        output.writeByte(0x00); // last section number
        output.writeShort(0xE000 | VIDEO_PID); // reserved + PCR PID
        output.writeShort(0xF000); // reserved + program info length (0)

        // stream entry - H.264 video
        output.writeByte(STREAM_TYPE_H264);
        output.writeShort(0xE000 | VIDEO_PID);
        output.writeShort(0xF000); // reserved + ES info length (0)

        // CRC32
        output.writeInt(Checksum.crc32(
                Checksum.CRC32_MPEG2, output.nioBuffer(tableStart, output.writerIndex() - tableStart)));

        // fill rest with 0xFF
        int remaining = TS_PACKET_SIZE - (output.writerIndex() - start);
        for (int i = 0; i < remaining; i++) {
            output.writeByte(0xFF);
        }
    }

    private ByteBuf createPes(ByteBuf nalData, long pts90k) {

        ByteBuf pes = Unpooled.buffer();

        // PES header
        pes.writeMedium(0x000001); // start code prefix
        pes.writeByte(0xE0); // stream id (video)

        int pesLength = nalData.readableBytes() + 14; // 3 (flags) + 5 (PTS) + 6 (AUD NAL)
        if (pesLength > 65535) {
            pes.writeShort(0x0000); // unbounded
        } else {
            pes.writeShort(pesLength);
        }

        pes.writeByte(0x80); // marker bits
        pes.writeByte(0x80); // PTS only
        pes.writeByte(0x05); // PES header data length

        // PTS encoding (5 bytes)
        pes.writeByte(0x21 | (int) ((pts90k >> 29) & 0x0E));
        pes.writeByte((int) (pts90k >> 22));
        pes.writeByte(0x01 | (int) ((pts90k >> 14) & 0xFE));
        pes.writeByte((int) (pts90k >> 7));
        pes.writeByte(0x01 | (int) ((pts90k << 1) & 0xFE));

        // access unit delimiter NAL
        pes.writeInt(0x00000001); // start code
        pes.writeByte(0x09); // AUD NAL type
        pes.writeByte(0xF0); // primary_pic_type = 7 (any) + rbsp stop bit

        pes.writeBytes(nalData, nalData.readerIndex(), nalData.readableBytes());

        return pes;
    }

    private void writePesPackets(ByteBuf output, ByteBuf pesData, boolean isKeyFrame, long pts90k) {
        int offset = 0;
        boolean first = true;
        int pesLength = pesData.readableBytes();

        while (offset < pesLength) {
            int packetStart = output.writerIndex();

            // sync byte
            output.writeByte(0x47);

            // PID with payload unit start flag
            int pidFlags = VIDEO_PID;
            if (first) {
                pidFlags |= 0x4000; // payload unit start
            }
            output.writeShort(pidFlags);

            int headerSize = 4;

            if (first && isKeyFrame) {
                // adaptation field with PCR and random access indicator
                output.writeByte(0x30 | (videoContinuityCounter++ & 0x0F)); // adaptation + payload
                output.writeByte(0x07); // adaptation field length
                output.writeByte(0x50); // random access indicator + PCR flag
                // PCR base (33 bits) + reserved (6) + extension (9)
                output.writeByte((int) (pts90k >> 25));
                output.writeByte((int) (pts90k >> 17));
                output.writeByte((int) (pts90k >> 9));
                output.writeByte((int) (pts90k >> 1));
                output.writeByte((int) (((pts90k & 1) << 7) | 0x7E)); // reserved bits
                output.writeByte(0x00); // extension
                headerSize += 8;
            } else {
                int remaining = pesLength - offset;
                int available = TS_PACKET_SIZE - 4;
                if (remaining < available) {
                    // need stuffing via adaptation field
                    int stuffingLength = available - remaining;
                    output.writeByte(0x30 | (videoContinuityCounter++ & 0x0F));
                    if (stuffingLength == 1) {
                        output.writeByte(0x00);
                        headerSize += 1;
                    } else {
                        output.writeByte(stuffingLength - 1);
                        output.writeByte(0x00); // no flags
                        for (int i = 0; i < stuffingLength - 2; i++) {
                            output.writeByte(0xFF);
                        }
                        headerSize += stuffingLength;
                    }
                } else {
                    output.writeByte(0x10 | (videoContinuityCounter++ & 0x0F)); // payload only
                }
            }

            int payloadSize = Math.min(pesLength - offset, TS_PACKET_SIZE - headerSize);
            output.writeBytes(pesData, pesData.readerIndex() + offset, payloadSize);

            // fill remaining with 0xFF
            int remaining = TS_PACKET_SIZE - (output.writerIndex() - packetStart);
            for (int i = 0; i < remaining; i++) {
                output.writeByte(0xFF);
            }

            offset += payloadSize;
            first = false;
        }
    }

}
