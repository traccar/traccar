package org.traccar.helper;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class ChecksumTest {

    @Test
    public void testCrc16() {
        ChannelBuffer buf = ChannelBuffers.copiedBuffer("123456789", StandardCharsets.US_ASCII);

        Assert.assertEquals(0x906e, Checksum.crc16(Checksum.CRC16_X25, buf.toByteBuffer()));
        Assert.assertEquals(0x29b1, Checksum.crc16(Checksum.CRC16_CCITT_FALSE, buf.toByteBuffer()));
        Assert.assertEquals(0x2189, Checksum.crc16(Checksum.CRC16_KERMIT, buf.toByteBuffer()));
        Assert.assertEquals(0x31c3, Checksum.crc16(Checksum.CRC16_XMODEM, buf.toByteBuffer()));
        Assert.assertEquals(0xe5cc, Checksum.crc16(Checksum.CRC16_AUG_CCITT, buf.toByteBuffer()));
        Assert.assertEquals(0xd64e, Checksum.crc16(Checksum.CRC16_GENIBUS, buf.toByteBuffer()));
        Assert.assertEquals(0x6f91, Checksum.crc16(Checksum.CRC16_MCRF4XX, buf.toByteBuffer()));
    }
    
    @Test
    public void testLuhn() {
        Assert.assertEquals(7, Checksum.luhn(12345678901234L));
        Assert.assertEquals(0, Checksum.luhn(63070019470771L));
    }

}
