package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class S168ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new S168ProtocolDecoder(null);

        verifyPosition(decoder, text(
                "S168#358511039001705#003a#01ca#LOCA:G;CELL:6,1cc,0,2479,de11150,2e,2479,d6e4546,31,2479,d6e4547,39,778c,787cc30,39,778c,787cc31,40,253f,6195502,32;GDATA:A,5,220117220950,22.779583,113.820633,5,296,35;ALERT:0080;STATUS:98,73;;FARM:0,0009,0000,010a,20220117220950;WIFI:10,CC-08-FB-A5-49-B3,-28,08-40-F3-7F-6C-A9,-59,A4-29-40-65-2C-42,-74,80-89-17-A5-6F-7B,-82,80-EA-07-82-93-C6,-82,FC-37-2B-34-D6-A1,-83,34-6B-5B-A9-49-15,-83,BC-46-99-B3-51-10,-84,BC-54-FC-53-0A-D1,-84,3C-CD-57-67-D1-32,-85"));

        verifyNull(decoder, text(
                "S168#358511139046180#00c9#0009#SYNC:0000"));

        verifyPosition(decoder, text(
                "S168#000000000000008#0f12#0077#LOCA:G;CELL:1,1cc,2,2795,1435,64;GDATA:A,12,160412154800,22.564025,113.242329,5.5,152,900;ALERT:0000;STATUS:89,98;WAY:0"));

    }

}
