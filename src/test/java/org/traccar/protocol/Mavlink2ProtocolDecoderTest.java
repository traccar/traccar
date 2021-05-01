package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Mavlink2ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new Mavlink2ProtocolDecoder(null);

        byte[] pkt = { 
        		// Packet start marker
        		(byte) 0xfd,
        		// Payload length
        		(byte) 0x1c, 
        		// Incompatibility Flags
        		(byte) 0x00, 
        		// Compatibility Flags
        		(byte) 0x00, 
        		// Packet sequence
        		(byte) 0x74, 
        		// System ID (sender)
        		(byte) 0x01, 
        		// Component ID (sender)
        		(byte) 0x01,
        		// Message ID (low, middle, high bytes)
        		(byte) 0x21, (byte) 0x00, (byte) 0x00,
        		// Payload Message data
        		  // Timestamp (time since system boot).
        		  (byte) 0xcc, (byte) 0xae, (byte) 0x08, (byte) 0x00, 
        		  // degE7 Latitude 
        		  (byte) 0x40, (byte) 0x05, (byte) 0xd3, (byte) 0x23, 
        		  // degE7 Longitude
        		  (byte) 0xb8, (byte) 0x9a, (byte) 0xa3, (byte) 0x0e, 
        		  // mm Altitude (MSL)
        		  (byte) 0x2e, (byte) 0xd0, (byte) 0x06, (byte) 0x00, 
        		  // mm Altitude above ground
        		  (byte) 0x67, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
        		  // cm/s Ground X Speed
        		  (byte) 0x8b, (byte) 0xff,
        		  // cm/s Ground Y Speed
        		  (byte) 0x05, (byte) 0x00,
        		  // cm/s Ground Z Speed
        		  (byte) 0x03, (byte) 0x00, 
        		  // cdeg Vehicle heading (yaw angle)
        		  (byte) 0x4f, (byte) 0x2d, 
        		// Checksum (low byte, high byte)
        		(byte) 0x00, (byte) 0x00 
        		};
        verifyPosition(decoder, pkt);

    }

}
