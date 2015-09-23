package org.traccar.protocol;

import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class Pt3000ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Pt3000ProtocolDecoder decoder = new Pt3000ProtocolDecoder(new Pt3000Protocol());

        verify(decoder.decode(null, null,
                "%356939010012099,$GPRMC,124945.752,A,4436.6245,N,01054.4634,E,0.11,358.52,060408,,,A,+393334347445,N028d"));

        verify(decoder.decode(null, null,
                "%356939010014433,$GPRMC,172821.000,A,4019.5147,N,00919.1160,E,0.00,,010613,,,A,+393998525043,N098d"));

    }

}
