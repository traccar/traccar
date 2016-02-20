package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class TelicProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        TelicProtocolDecoder decoder = new TelicProtocolDecoder(new TelicProtocol());

        verifyPosition(decoder, text(
                "002017297899,190216202500,0,190216202459,014221890,46492170,3,0,0,6,,,1034,43841,,0000,00,0,209,0,0407\u0000"));

        verifyPosition(decoder, text(
                "182043672999,010100001301,0,270613041652,166653,475341,3,0,355,6,2,1,231,8112432,23201,01,00,217,0,0,0,0,7\u0000"),
                position("2013-06-27 04:16:52.000", true, 47.53410, 16.66530));

        verifyPosition(decoder, text(
                "182043672999,010100001301,0,270613041652,166653,475341,3,0,355,6,2,1,231,8112432,23201,01,00,217,0,0,0,0,7\u0000"));

    }

}
