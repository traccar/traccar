package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class CarscopProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        CarscopProtocolDecoder decoder = new CarscopProtocolDecoder(new CarscopProtocol());

        verify(decoder.decode(null, null,
                "*040331141830UB05123456789012345061825A2934.0133N10627.2544E000.0040331309.6200000000L000000"));

        verify(decoder.decode(null, null,
                "*040331141830UB04999999984061825A2934.0133N10627.2544E000.0040331309.6200000000L000000"));

        verify(decoder.decode(null, null,
                "*040331141830UA012Hi-jack061825A2934.0133N10627.2544E000.0040331309.6200000000L000000"));

    }

}
