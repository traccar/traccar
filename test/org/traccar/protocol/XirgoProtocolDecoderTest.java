package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class XirgoProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        XirgoProtocolDecoder decoder = new XirgoProtocolDecoder(null);

        verify(decoder.decode(null, null, null,
                "$$354898045650537,6031,2015/02/26,15:47:26,33.42552,-112.30308,287.8,0,0,0,0,0.0,7,1.2,2,0.0,12.2,22,1,0,82.3"));

    }

}
