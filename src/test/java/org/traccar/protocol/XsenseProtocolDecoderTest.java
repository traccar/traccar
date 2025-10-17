package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class XsenseProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        inject(new XsenseProtocolDecoder(null));

        // TODO: Add real test data based on actual xsense protocol packets
        // These are placeholder tests - replace with actual hex data from xsense devices
        
        // Test extended position report
        // verifyPosition(decoder, binary(
        //     "10..."));

        // Test batch position report
        // verifyPositions(decoder, binary(
        //     "11..."));

    }

}
