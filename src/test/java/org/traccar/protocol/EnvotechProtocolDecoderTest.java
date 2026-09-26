package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class EnvotechProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new EnvotechProtocolDecoder(null));

        verify(decoder, text(
                "$80IVM,03,E002215,E002215,110422061936,672763902,126423,0180,000000,00018600,0.0000'11042206193710406325S03966094E000118*42D6#"),
                position());

        verify(decoder, text(
                "$80SLM,62,000F,F015727,020522053200,611000000,000391,C080,000,80028900,85010000'02052205320110399426S03970217E000145*E0C2#"),
                position());

        verify(decoder, text(
                "$80SLM,02,F,AB0010,130410155921,431750216,000040,0000,,00000000,'13041015592110476673N10111459E001281*2A"),
                position().location("2010-04-13T15:59:21.000Z", true, 4.76673, 101.11459));

        verify(decoder, text(
                "$80SLM,82,F,AB0010,130410155921,431750216,000040,0000,,00000000,'13041015592110476673N10111459E001281@B0,F,C456,038,00,M234567,,,1A2A3A4A5A6A*4E"),
                position());

        verify(decoder, text(
                "$80SLM,60,000F,F016109,290322100445,AF2463902,000406,0000,000,00018780,54000000'29032209493500406302S03966062E000348*E637"),
                position());

    }

}
