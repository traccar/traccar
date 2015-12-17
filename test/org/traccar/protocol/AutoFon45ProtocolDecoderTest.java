package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;


public class AutoFon45ProtocolDecoderTest extends ProtocolTest {
    @Test
    public void testDecode() throws Exception {
        AutoFon45ProtocolDecoder decoder = new AutoFon45ProtocolDecoder(new AutoFon45Protocol());

        verifyNothing(decoder, binary(
                "41035151305289931441139602662095148807"));

        verifyNothing(decoder, binary(
                "41032125656985547543619173484002123481"));

        verifyPosition(decoder, binary(
                "023E00001E004D411EFA01772F185285009C48041F1E366C2961380F26B10B00911C"),
                position("2010-01-27 04:00:08.000", true, 54.73838, 56.10343));

        verifyPosition(decoder, binary(
                "023E00001E004D411EFA01772F185285009C48041F1E366C2961380F26B10B00911C"));
    }
}
