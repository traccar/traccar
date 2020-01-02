package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class RstProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        RstProtocolDecoder decoder = new RstProtocolDecoder(null);

        verifyAttribute(decoder, text(
                "RST;A;RST-MINIv2;V7.00;008033985;1;7;30-08-2019 11:31:38;30-08-2019 11:31:15;-23.645868;-46.637741;0;226;828;0;10;0;00;20;00;1A;02;0.02;3.40;0;0;FE;0000;04;80;11;0;FIM;"),
                Position.KEY_BATTERY, 3.40);

        verifyPosition(decoder, text(
                "RST;A;RST-MINIv2;V7.00;008033985;1;7;30-08-2019 11:31:38;30-08-2019 11:31:15;-23.645868;-46.637741;0;226;828;0;10;0;00;20;00;1A;02;0.02;3.40;0;0;FE;0000;04;80;11;0;FIM;"));

        verifyPosition(decoder, text(
                "RST;A;RST-MINIv2;V7.00;008033985;6;47;30-08-2019 19:01:13;30-08-2019 19:01:14;-23.645851;-46.637817;0;294;811;1;11;0;00;30;00;1A;02;3.82;4.16;0;0;FE;0000;02;40;71;000001F60A55;FIM;"));

    }

}
