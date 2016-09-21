package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class CguardProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        CguardProtocolDecoder decoder = new CguardProtocolDecoder(new CguardProtocol());

        verifyNothing(decoder, text(
                "IDRO:354868050655283"));

        verifyPosition(decoder, text(
                "NV:160711 044023:54.342907:48.582590:0:NAN:0:110.1"));

        verifyPosition(decoder, text(
                "NV:160711 044023:54.342907:-148.582590:0:NAN:0:110.1"));

        verifyNothing(decoder, text(
                "BC:160711 044023:CSQ1:48:NSQ1:7:NSQ2:1:BAT1:98:PWR1:11.7:CLG1:NAN"));

        verifyNothing(decoder, text(
                "BC:160711 044524:CSQ1:61:NSQ1:18:BAT1:98:PWR1:11.7:CLG1:NAN"));

        verifyNothing(decoder, text(
                "VERSION:3.3"));

        verifyPosition(decoder, text(
                "NV:160420 101902:55.799425:37.674033:0.94:NAN:213.59:156.6"));

        verifyNothing(decoder, text(
                "BC:160628 081024:CSQ1:32:NSQ1:10:BAT1:100"));

        verifyNothing(decoder, text(
                "BC:160628 081033:NSQ2:0"));

        verifyPosition(decoder, text(
                "NV:160630 151537:55.799913:37.674267:0.7:NAN:10.21:174.9"));

        verifyNothing(decoder, text(
                "BC:160630 153316:BAT1:76"));

        verifyNothing(decoder, text(
                "BC:160630 153543:NSQ2:0"));

        verifyNothing(decoder, text(
                "PING"));

    }

}
