package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class HaicomProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        HaicomProtocolDecoder decoder = new HaicomProtocolDecoder(new HaicomProtocol());

        verify(decoder.decode(null, null,
                "$GPRS012497007097169,T100001,150618,230031,5402267400332464,0004,2014,000001,,,1,00#V040*"));

        verify(decoder.decode(null, null,
                "$GPRS123456789012345,602S19A,100915,063515,7240649312041079,0019,3156,111000,10004,0000,11111,00LH#V037"));
        
        verify(decoder.decode(null, null,
                "$GPRS123456789012345,T100001,141112,090751,7240649312041079,0002,1530,000001,,,1,00#V039*"));
        
        verify(decoder.decode(null, null,
                "$GPRS012497007101250,T100001,141231,152235,7503733600305643,0000,2285,000001,,,1,00#V041*"));

    }

}
