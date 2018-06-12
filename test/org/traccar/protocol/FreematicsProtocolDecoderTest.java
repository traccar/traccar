package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class FreematicsProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        FreematicsProtocolDecoder decoder = new FreematicsProtocolDecoder(new FreematicsProtocol());

        verifyPosition(decoder, text(
                "1#0=68338,10D=79,30=1010,105=199,10C=4375,104=56,111=62,20=0;-1;95,10=6454200,A=-32.727482,B=150.150301,C=159,D=0,F=5,24=1250*7A"));

    }

}
