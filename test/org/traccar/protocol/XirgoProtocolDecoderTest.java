package org.traccar.protocol;

import org.junit.Test;
import static org.traccar.helper.DecoderVerifier.verify;

public class XirgoProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        XirgoProtocolDecoder decoder = new XirgoProtocolDecoder(new XirgoProtocol());

        verify(decoder.decode(null, null,
                "$$354660046140722,6001,2013/01/22,15:36:18,25.80907,-80.32531,7.1,19,165.2,11,0.8,11.1,17,1,1,3.9,2##"));

        verify(decoder.decode(null, null,
                "$$357207059646786,4003,2015/05/19,15:54:56,-20.21422,-70.14927,37.5,1.8,0.0,11,0.8,12.9,31,297,1,0,0.0,0.0,0,1,1,1##"));

        verify(decoder.decode(null, null,
                "$$354898045650537,6031,2015/02/26,15:47:26,33.42552,-112.30308,287.8,0,0,0,0,0.0,7,1.2,2,0.0,12.2,22,1,0,82.3"));

        verify(decoder.decode(null, null,
                "$$357207059646786,4003,2015/05/19,15:55:27,-20.21421,-70.14920,33.6,0.4,0.0,11,0.8,12.9,31,297,1,0,0.0,0.0,0,1,1,1##"));

    }

}
