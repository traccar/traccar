package org.traccar.helper;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class ChecksumTest {

    @Test
    public void testCrc8() {
        ChannelBuffer buf = ChannelBuffers.copiedBuffer("123456789", StandardCharsets.US_ASCII);

        assertEquals(0xF7, Checksum.crc8(Checksum.CRC8_EGTS, buf.toByteBuffer()));
        assertEquals(0xD0, Checksum.crc8(Checksum.CRC8_ROHC, buf.toByteBuffer()));
    }

    @Test
    public void testCrc16() {
        ChannelBuffer buf = ChannelBuffers.copiedBuffer("123456789", StandardCharsets.US_ASCII);

        assertEquals(0xBB3D, Checksum.crc16(Checksum.CRC16_IBM, buf.toByteBuffer()));
        assertEquals(0x4B37, Checksum.crc16(Checksum.CRC16_MODBUS, buf.toByteBuffer()));
        assertEquals(0x906e, Checksum.crc16(Checksum.CRC16_X25, buf.toByteBuffer()));
        assertEquals(0x29b1, Checksum.crc16(Checksum.CRC16_CCITT_FALSE, buf.toByteBuffer()));
        assertEquals(0x2189, Checksum.crc16(Checksum.CRC16_KERMIT, buf.toByteBuffer()));
        assertEquals(0x31c3, Checksum.crc16(Checksum.CRC16_XMODEM, buf.toByteBuffer()));
    }

    @Test
    public void testLuhn() {
        assertEquals(7, Checksum.luhn(12345678901234L));
        assertEquals(0, Checksum.luhn(63070019470771L));
    }

}
