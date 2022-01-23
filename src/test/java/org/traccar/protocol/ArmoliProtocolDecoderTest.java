package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class ArmoliProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new ArmoliProtocolDecoder(null);

        verifyPosition(decoder, text(
                "[M869867038698074210122125205N38.735641E035.4727751E003340000000C00000E9E07FF:106AG505283H60E]"));

        verifyAttribute(decoder, text(
                        "[W869867038698074,O,1234,2657,1]"),
                Position.KEY_RESULT, "O,1234,2657,1");

        verifyNull(decoder, text(
                "[Q010001088610010024363698990011101070608200,05XXXXXXXXX,10.49.182.53,C,1,20,19,0]"));

        verifyPosition(decoder, text(
                "[M860906041293587100122061310N40.792751E029.4313092801143000000010003513209FFGC18080H8DA#E209C4]"));

        verifyNull(decoder, text(
                "[L866104027971681]"));

    }

}
