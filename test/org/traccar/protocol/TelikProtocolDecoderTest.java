package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

public class TelikProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        TelikProtocolDecoder decoder = new TelikProtocolDecoder(new TelikProtocol());

        verifyNothing(decoder, text(
                "0026436729|232|01|003002030"));

        verifyPosition(decoder, text(
                "182043672999,010100001301,0,270613041652,166653,475341,3,0,355,6,2,1,231,8112432,23201,01,00,217,0,0,0,0,7"),
                position("2013-06-27 04:16:52.000", true, 47.53410, 16.66530));

        verifyPosition(decoder, text(
                "182043672999,010100001301,0,270613041652,166653,475341,3,0,355,6,2,1,231,8112432,23201,01,00,217,0,0,0,0,7"));

    }

}
