package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class TrackboxProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new TrackboxProtocolDecoder(null);

        verifyNull(decoder, text(
                "a=connect&v=11&i=111111111111111"));

        verifyPosition(decoder, text(
                "183457.999,5126.0247N,00002.8686E,5.2,70.4,3,57.63,32.11,17.32,150507,05"),
                position("2007-05-15 18:34:57.999", true, 51.43375, 0.04781));

        verifyPosition(decoder, text(
                "183558.999,5126.3979N,00003.0745E,5.2,70.4,3,57.63,32.11,17.32,150507,05"));

    }

}
