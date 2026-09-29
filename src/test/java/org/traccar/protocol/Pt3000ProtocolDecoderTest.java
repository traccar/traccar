package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Pt3000ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Pt3000ProtocolDecoder(null));

        verifyDecode(decoder, text(
                "%356939010012099,$GPRMC,124945.752,A,4436.6245,N,01054.4634,E,0.11,358.52,060408,,,A,+393334347445,N028d"),
                position().location("2008-04-06T12:49:45.000Z", true, 44.61041, 10.90772));

        verifyDecode(decoder, text(
                "%356939010014433,$GPRMC,172821.000,A,4019.5147,N,00919.1160,E,0.00,,010613,,,A,+393998525043,N098d"),
                position());

    }

}
