package org.traccar.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class Itr120ProtocolDecoderTest {

    @Test
    public void testIgnitionOnBit() {
        assertEquals(Boolean.TRUE, Itr120ProtocolDecoder.decodeIgnition(0x0007));
    }

    @Test
    public void testIgnitionOffBit() {
        assertEquals(Boolean.FALSE, Itr120ProtocolDecoder.decodeIgnition(0x0003));
    }

    @Test
    public void testIgnitionIgnoredWhenNotCar() {
        assertNull(Itr120ProtocolDecoder.decodeIgnition(0x0005));
    }

}
