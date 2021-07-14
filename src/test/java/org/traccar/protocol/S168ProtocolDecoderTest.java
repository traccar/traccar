package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class S168ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new S168ProtocolDecoder(null);

        verifyNull(decoder, text(
                "S168#358511139046180#00c9#0009#SYNC:0000"));

        verifyPosition(decoder, text(
                "S168#000000000000008#0f12#0077#LOCA:G;CELL:1,1cc,2,2795,1435,64;GDATA:A,12,160412154800,22.564025,113.242329,5.5,152,900;ALERT:0000;STATUS:89,98;WAY:0"));

    }

}
