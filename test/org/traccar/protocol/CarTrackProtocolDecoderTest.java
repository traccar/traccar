package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class CarTrackProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        CarTrackProtocolDecoder decoder = new CarTrackProtocolDecoder(null);

        verify(decoder.decode(null, null,
                "$$2222234???????&A9955&B102904.000,A,2233.0655,N,11404.9440,E,0.00,,030109,,*17|6.3|&C0100000100&D000024?>&E10000000&Y00100020"));
        
        verify(decoder.decode(null, null,
                "$$2222234???????&A9955&B102904.000,A,2233.0655,N,11404.9440,E,0.00,,030109,,*17|6.3|&C0100000100&D000024?>&E10000000"));


    }

}
