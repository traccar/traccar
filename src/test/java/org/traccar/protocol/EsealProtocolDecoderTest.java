package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class EsealProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new EsealProtocolDecoder(null));

        verifyPosition(decoder, text(
                "##S,eSeal,1000821,256,3.0.6,Normal,34,2017-08-31,08:14:40,15,A,25.708828N 100.372870W,10,0,Close,0.71,0:0:3:0,3.8,-73,E##"));

        verifyPosition(decoder, text(
                "##S,eSeal,1000821,256,3.0.6,Startup,1,2017-08-31,02:01:19,3,V,0.000000N 0.000000E,0,0,Close,3.25,0:0:5:0,3.8,-93,E##"));

        verifyNull(decoder, text(
                "##S,eSeal,1000821,256,3.0.6,Startup OK,1,180,30,30,16,1,E##"));

        verifyNull(decoder, text(
                "##S,eSeal,1000821,256,3.0.6,Startup OK,1,180,30,30,16,1,E##"));

        verifyPosition(decoder, text(
                "##S,eSeal,1000898,256,3.0.6,Normal,6,2017-09-06,23:48:39,3,V,0.000000N 0.000000E,0,0,Close,1.0,0:0:3:0,4.0,-81,E##"));

        verifyNull(decoder, text(
                "##S,eSeal,1000898,256,3.0.6,RC-NFC DEL ACK,,E##"));

    }

}
