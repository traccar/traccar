package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class MictrackLowAltitudeFlightProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        MictrackLowAltitudeFlightProtocolDecoder decoder = new MictrackLowAltitudeFlightProtocolDecoder(null);

        verifyPositions(decoder, text(
                "861108032038761$062232.00,A,2238.2832,N,11401.7381,E,0.01,309.62,95.0,131117"));

        verifyPositions(decoder, text(
                "861108032038761$062232.00,A,2238.2832,N,11401.7381,E,0.01,309.62,95.0,131117$"+
                "062332.00,A,2238.2836,N,11401.7386,E,0.06,209.62,95.0,131117"));

        verifyPositions(decoder, text(
                "861108032038761$062232.00,A,2238.2832,N,11401.7381,E,0.01,309.62,95.0,131117"),
                position("2017-11-13 06:22:32.000", true, 22.63806, 114.028976));
    }

}
