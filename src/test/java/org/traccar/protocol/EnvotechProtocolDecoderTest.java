package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class EnvotechProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new EnvotechProtocolDecoder(null);

        verifyPosition(decoder, text(
                "$80SLM,02,F,AB0010,130410155921,431750216,000040,0000,,00000000,'13041015592110476673N10111459E001281*2A"));

        verifyPosition(decoder, text(
                "$80SLM,82,F,AB0010,130410155921,431750216,000040,0000,,00000000,'13041015592110476673N10111459E001281@B0,F,C456,038,00,M234567,,,1A2A3A4A5A6A*4E"));

        verifyPosition(decoder, text(
                "$80SLM,60,000F,F016109,290322100445,AF2463902,000406,0000,000,00018780,54000000'29032209493500406302S03966062E000348*E637"));

    }

}
