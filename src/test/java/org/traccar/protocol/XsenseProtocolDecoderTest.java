package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XsenseProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new XsenseProtocolDecoder(null));

        // Real xsense packet - Type 114 (0x72) = M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO
        // This packet contains 6 position records + base station data
        // Device ID from packet: TID = 0xC29A92 (hex) = 12753042 (dec)
        verifyNull(decoder, binary(
                "72ad3ac5bd7d3fae3c60abba63f85426ba4d64f85126ba2665f85026ba0866f84f26" +
                "ba5566f84e26ba4367f84e26ba0d2800010db1b3110101010001002bc5b32e202020" +
                "202020202020202020202020202020202020202020202020202020202020dcb5"));

        // Additional test with second packet
        verifyNull(decoder, binary(
                "72ad3ac5bd7d3fa9abc463f854264bad64f85426ba2d65f85326ba0f66f84f26ba31" +
                "67f84f26ba5368f84e26ba0c2800010db1b3110101010001002bc5b32e2020202020" +
                "20202020202020202020202020202020202020202020202020202020c4b2"));
    }

    @Test
    public void testBatchOfflineEnhIo() throws Exception {

        var decoder = inject(new XsenseProtocolDecoder(null));

    // Prime mocked connection manager so subsequent lookups return a DeviceSession
    assertNotNull(decoder.getDeviceSession(null, null, "bootstrap", "10D092"));

        @SuppressWarnings("unchecked")
        List<Position> positions = (List<Position>) decoder.decode(null, null, binary(
                "73D7B8BFC70745AED1C01AD22E5C19D73737D75FC1750BD3D1C01AF22E5C1BD7" +
                "3737D75FC1750C3BD1C01AA22E5C1AD73737D75FC1750C0FD1C01A822E5C19D7" +
                "3737D75741750C13D1C01A722E5C18D73737D65F41750C7BD1C01A622E5C19D7" +
                "3737D757C1750C4FD6CDD7D7D7D7D7D76010"));

        Position expected = position("2025-10-17 13:48:04.000", true, 6.648240, 100.400557);

        assertNotNull(positions, "positions list is null");
        assertEquals(6, positions.size(), "unexpected record count");

        Position first = positions.get(0);
        assertEquals(expected.getLatitude(), first.getLatitude(), 0.00001);
        assertEquals(expected.getLongitude(), first.getLongitude(), 0.00001);
        assertEquals(expected.getFixTime(), first.getFixTime());
        assertTrue(first.getValid(), "first record should be valid");
    }

    @Test
    public void testSiemensGps32Format() throws Exception {

        var decoder = inject(new XsenseProtocolDecoder(null));

        // Real Siemens packet from tcpdump: 7E7E7E7E00726D242C7E11F4080033520D9909002A903EDBAC70FEF9000B289700000000078800000063D3CA7E7E
        // After removing preamble (7E7E7E7E00) and trailer (7E7E):
        // Type=72(114), Seq=6D, Size=24, BoxID=2C7E (11390), 1x GPS32 record (32 bytes) + CRC
        // This is raw Siemens format (no XOR encoding)
        // BoxID 0x2C7E (11390) + 1200000 = device 1211390
        verifyPositions(decoder, binary(
                "726D242C7E11F4080033520D9909002A903EDBAC70FEF9000B289700000000078800000063D3CA"));

    }

}