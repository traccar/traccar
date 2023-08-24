package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class B2316ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new B2316ProtocolDecoder(null));

        verifyPositions(decoder, false, text(
                "{\"imei\":\"866349041783600\",\"data\":[{\"tm\":1631162952,\"wn\":7},{\"tm\":1631158729,\"ic\":\"89883030000059398609\",\"ve\":\"B2316.TAU.U.TH01\"},{\"tm\":1631158805,\"te\":\"312,363\",\"st\":0,\"ba\":3,\"sn\":80},{\"tm\":1631158829,\"ci\":\"505,1,8218,133179149,-108\"},{\"tm\":1631162956,\"wi\":\"101331c17f4f,-74;f46bef7953bb,-81;b09575cff1c8,-86;e2b9e5d61a7a,-88;b0ee7b4dee2f,-88;e0b9e5d61a77,-89;f66bef7953b9,-89;\",\"te\":\"335,366\",\"hr\":58,\"bp\":\"113,73\",\"st\":0,\"ba\":3,\"sn\":60},{\"tm\":1631162968,\"ci\":\"505,1,8218,133179149,-105\"}]}"));

    }

}
