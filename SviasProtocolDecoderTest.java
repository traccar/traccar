package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class SviasProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        SviasProtocolDecoder decoder = new SviasProtocolDecoder(new SviasProtocol());

        verifyPosition(decoder, text(
                "7051,3041,1121,30179295,710,300518,40443,-93429140,-354560540,0,23437,3983,0,1,0,0,12433,100,22,32,4898"));

    }

}
