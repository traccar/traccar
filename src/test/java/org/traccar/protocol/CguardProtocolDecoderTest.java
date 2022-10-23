package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class CguardProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new CguardProtocolDecoder(null));

        verifyNull(decoder, text(
                "IDRO:354868050655283"));

        verifyPosition(decoder, text(
                "NV:190225 144543:55.776505:37.729000:0:10:0:0"));

        verifyAttributes(decoder, text(
                "BC:190225 144543:CSQ1:64:NSQ1:0:NSQ2:1:BAT1:100"));

        verifyAttributes(decoder, text(
                "BC:190225 142653:CSQ1:80:NSQ1:0:NSQ2:1:BAT1:3.53"));

        verifyPosition(decoder, text(
                "NV:170409 031456:56.808553:60.595476:0:NAN:0"));

        verifyAttributes(decoder, text(
                "BC:170409 031456:CSQ1:64:NSQ1:17:PWR1:0"));

        verifyPosition(decoder, text(
                "NV:161007 122043:55.812730:37.733689:3.62:NAN:244.05:143.4"));

        verifyPosition(decoder, text(
                "NV:161007 122044:55.812732:37.733670:3.97:NAN:260.95:143.9"));

        verifyAttributes(decoder, text(
                "BC:161007 122044:CSQ1:77:NSQ1:18:BAT1:100"));

        verifyPosition(decoder, text(
                "NV:160711 044023:54.342907:48.582590:0:NAN:0:110.1"));

        verifyPosition(decoder, text(
                "NV:160711 044023:54.342907:-148.582590:0:NAN:0:110.1"));

        verifyAttributes(decoder, text(
                "BC:160711 044023:CSQ1:48:NSQ1:7:NSQ2:1:BAT1:98:PWR1:11.7:CLG1:NAN"));

        verifyAttributes(decoder, text(
                "BC:160711 044524:CSQ1:61:NSQ1:18:BAT1:98:PWR1:11.7:CLG1:NAN"));

        verifyNull(decoder, text(
                "VERSION:3.3"));

        verifyPosition(decoder, text(
                "NV:160420 101902:55.799425:37.674033:0.94:NAN:213.59:156.6"));

        verifyAttributes(decoder, text(
                "BC:160628 081024:CSQ1:32:NSQ1:10:BAT1:100"));

        verifyAttributes(decoder, text(
                "BC:160628 081033:NSQ2:0"));

        verifyPosition(decoder, text(
                "NV:160630 151537:55.799913:37.674267:0.7:NAN:10.21:174.9"));

        verifyAttributes(decoder, text(
                "BC:160630 153316:BAT1:76"));

        verifyAttributes(decoder, text(
                "BC:160630 153543:NSQ2:0"));

        verifyNull(decoder, text(
                "PING"));

    }

}
