package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Pt3000ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new Pt3000ProtocolDecoder(null);

        verifyPosition(decoder, text(
                "%356939010012099,$GPRMC,124945.752,A,4436.6245,N,01054.4634,E,0.11,358.52,060408,,,A,+393334347445,N028d"),
                position("2008-04-06 12:49:45.000", true, 44.61041, 10.90772));

        verifyPosition(decoder, text(
                "%356939010014433,$GPRMC,172821.000,A,4019.5147,N,00919.1160,E,0.00,,010613,,,A,+393998525043,N098d"));

    }

}
