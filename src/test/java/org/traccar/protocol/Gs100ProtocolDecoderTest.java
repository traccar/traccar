package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Gs100ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new Gs100ProtocolDecoder(null);

        verifyNull(decoder, binary(
                "474C490F383632343632303332353036373030133839333831303131363039313838343837323546084657312E302E3236"));

        verifyPositions(decoder, binary(
                "47440216900000064113030417020236402C452286650051929716900000064115030417020236408C4522866800379020"));

    }

}
