package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

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

}

