package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class ArmoliProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new ArmoliProtocolDecoder(null);

        verifyNull(decoder, text(
                "[Q010001088610010024363698990011101070608200,05XXXXXXXXX,10.49.182.53,C,1,20,19,0];"));

        verifyPosition(decoder, text(
                "[M860906041293587100122061310N40.792751E029.4313092801143000000010003513209FFGC18080H8DA#E209C4];"));

        verifyNull(decoder, text(
                "[L866104027971681];"));

    }

}
