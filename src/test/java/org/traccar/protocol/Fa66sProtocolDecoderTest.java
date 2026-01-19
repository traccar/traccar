package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Fa66sProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Fa66sProtocolDecoder(null));

        String message = text(
                "[3G*9519378879*01D2*UD_LTE,190126,033747,V,22.683546,N,113.9907380,E,0.00,0.0,0.0,0,100,8,670,0,00000001,1,0,460,1,30538,102075140,177,15,,82:68:F9:ED:03:A0,-39]");

        verifyPosition(decoder, message, position("2019-01-26 03:37:47.000", false, 22.683546, 113.9907380));

        Position position = (Position) decoder.decode(null, null, message);
        assertNotNull(position);
        assertEquals(1L, position.getDeviceId());
    }

}
