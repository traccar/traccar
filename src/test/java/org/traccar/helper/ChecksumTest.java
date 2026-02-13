package org.traccar.helper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChecksumTest {

    @Test
    public void testCrc8() {
        ByteBuf buf = Unpooled.copiedBuffer("123456789", StandardCharsets.US_ASCII);

        assertEquals(0xF7, Checksum.crc8(Checksum.CRC8_EGTS, buf.nioBuffer()));
        assertEquals(0xD0, Checksum.crc8(Checksum.CRC8_ROHC, buf.nioBuffer()));
    }

    @Test
    public void testCrc16() {
        ByteBuf buf = Unpooled.copiedBuffer("123456789", StandardCharsets.US_ASCII);

        assertEquals(0xBB3D, Checksum.crc16(Checksum.CRC16_IBM, buf.nioBuffer()));
        assertEquals(0x4B37, Checksum.crc16(Checksum.CRC16_MODBUS, buf.nioBuffer()));
        assertEquals(0x906e, Checksum.crc16(Checksum.CRC16_X25, buf.nioBuffer()));
        assertEquals(0x29b1, Checksum.crc16(Checksum.CRC16_CCITT_FALSE, buf.nioBuffer()));
        assertEquals(0x2189, Checksum.crc16(Checksum.CRC16_KERMIT, buf.nioBuffer()));
        assertEquals(0x31c3, Checksum.crc16(Checksum.CRC16_XMODEM, buf.nioBuffer()));
    }

    @Test
    public void testLuhn() {
        assertEquals(7, Checksum.luhn(12345678901234L));
        assertEquals(0, Checksum.luhn(63070019470771L));
    }

    @Test
    public void testModulo256() {
        assertEquals(0x00, Checksum.modulo256(ByteBuffer.wrap(new byte[] {0x00})));
        assertEquals(0x00, Checksum.modulo256(ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x00})));
        assertEquals(0xca, Checksum.modulo256(ByteBuffer.wrap(new byte[] {0x77, 0x77, 0x77, 0x77, 0x77, 0x77})));
    }

    @Test
    public void testNmea() {
        assertEquals("*2A", Checksum.nmea("GSC,011412000010789,M4(Ro=500)"));
    }

}
