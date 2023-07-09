package org.traccar.helper;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BcdUtilTest {
    
    @Test
    public void testReadInteger() {
        byte[] buf = {0x01, (byte) 0x90, 0x34};
        int result = BcdUtil.readInteger(Unpooled.wrappedBuffer(buf), 5);
        assertEquals(1903, result);
    }

    @Test
    public void testReadCoordinate() {
        byte[] buf = {0x03, (byte) 0x85, 0x22, 0x59, 0x34};
        double result = BcdUtil.readCoordinate(Unpooled.wrappedBuffer(buf));
        assertEquals(38.870989, result, 0.00001);
    }

}
