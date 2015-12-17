package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class YwtProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        YwtProtocolDecoder decoder = new YwtProtocolDecoder(new YwtProtocol());
        
        verifyNothing(decoder, text(
                "%SN,0417061042:0,0,140117041203,404"));

        verifyPosition(decoder, text(
                "%GP,3000012345:0,090723182813,E114.602345,N22.069725,,30,160,4,0,00,,2794-10FF-46000,3>0-0"));

        verifyPosition(decoder, text(
                "%RP,3000012345:0,090807182815,E114.602345,N22.069725,,30,160,4,0,00"),
                position("2009-08-07 18:28:15.000", true, 22.06973, 114.60235));

        verifyPosition(decoder, text(
                "%KP,3000012345:0,090807183115,E114.602345,N22.069725,,30,160,5,0,00;"));

    }

}
