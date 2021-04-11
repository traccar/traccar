package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class HaicomProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new HaicomProtocolDecoder(null);

        verifyPosition(decoder, text(
                "$GPRS012497007097169,T100001,150618,230031,5402267400332464,0004,2014,000001,,,1,00#V040*"),
                position("2015-06-18 23:00:31.000", true, 40.37790, -3.54107));

        verifyPosition(decoder, text(
                "$GPRS123456789012345,602S19A,100915,063515,7240649312041079,0019,3156,111000,10004,0000,11111,00LH#V037"));
        
        verifyPosition(decoder, text(
                "$GPRS123456789012345,T100001,141112,090751,7240649312041079,0002,1530,000001,,,1,00#V039*"));
        
        verifyPosition(decoder, text(
                "$GPRS012497007101250,T100001,141231,152235,7503733600305643,0000,2285,000001,,,1,00#V041*"));

    }

}
