package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Pt60ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Pt60ProtocolDecoder decoder = new Pt60ProtocolDecoder(new Pt60Protocol());

        verifyPosition(decoder, text(
                "@G#@,V01,6,111112222233333,8888888888888888,20150312010203,23.2014050;104.235212,"));

        verifyNull(decoder, text(
                "@G#@,V01,1,353882080015633,9460025014649193,"));

    }

}
