package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class ArnaviProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        ArnaviProtocolDecoder decoder = new ArnaviProtocolDecoder(new ArnaviProtocol());

        verifyPosition(decoder, text(
                "$AV,V2,32768,12487,2277,203,-1,0,0,193,0,0,1,13,200741,5950.6773N,03029.1043E,0.0,0.0,121012,*6E"));

        verifyPosition(decoder, text(
                "$AV,V3,999999,12487,2277,203,65534,0,0,193,65535,65535,65535,65535,1,13,200741,5950.6773N,03029.1043E,300.0,360.0,121012,65535,65535,65535,SF*6E"));

    }

}
