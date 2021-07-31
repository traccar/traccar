package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class SabertekProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new SabertekProtocolDecoder(null);

        verifyPosition(decoder, text(
                ",999999999,3,40,65,7,0,1,-25.781666,28.254702,40,268,1414,8,55623,"));

        verifyPosition(decoder, text(
                ",999999999,4,356495040613400,89270200120171498287,+27821234123,20180525145412,60,75,15,1,1,-25.781666,28.254702,40,268,1414,8,24844,"));

    }

}
