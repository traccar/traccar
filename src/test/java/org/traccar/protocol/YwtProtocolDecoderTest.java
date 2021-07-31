package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class YwtProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new YwtProtocolDecoder(null);

        verifyPosition(decoder, text(
                "%RP,1222102985:1,170509033842,E102.146563,N14.582175,,0,320,10,0,00-00-00-00-00-00-00-00-00-00-00-00,,1db2-02b3-52004,3>941.523-32,7>1,19>-16,20>30.9V"));
        
        verifyNull(decoder, text(
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
