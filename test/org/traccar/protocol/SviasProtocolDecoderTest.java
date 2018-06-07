package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class SviasProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        SviasProtocolDecoder decoder = new SviasProtocolDecoder(new SviasProtocol());                

        verifyPosition(decoder, text(
                  "[7061,3041,57,20324277,710,40618,141342,-93155840,-371754060,0,20469,0,16,1,0,0,11323,100,9,,32,4695]"));

    }

}
