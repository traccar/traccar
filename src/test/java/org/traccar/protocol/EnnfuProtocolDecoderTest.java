package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class EnnfuProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new EnnfuProtocolDecoder(null));

        verifyPosition(decoder, text(
                "Ennfu:354679095321652,041504.00,A,3154.86654,N,11849.08737,E,0.053,,080121,20,3.72,21.4,V0.01"));

        verifyPosition(decoder, text(
                "Ennfu:354679095321652,060951.00,A,3154.86786,N,11849.09042,E,0.058,,060121,17,4.0,86.8,V0.01"));

        verifyNull(decoder, text(
                "Ennfu:HeartBeat,354679095321652"));

    }

}
