package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class HaicomProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        HaicomProtocolDecoder decoder = new HaicomProtocolDecoder(new TestDataManager());

        verify(decoder.decode(null, null,
                "$GPRS123456789012345,602S19A,100915,063515,7240649312041079,0019,3156,111000,10004,0000,11111,00LH#V037"));

    }

}
