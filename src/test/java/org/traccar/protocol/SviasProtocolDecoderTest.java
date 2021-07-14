package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class SviasProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new SviasProtocolDecoder(null);

        verifyPosition(decoder, text(
                "[7061,3041,57,20324277,710,40618,141342,-93155840,-371754060,0,20469,0,16,1,0,0,11323,100,9,,32,4695"));

        verifyPosition(decoder, text(
                "[7041,3041,8629,20856286,710,60618,201027,-92268040,-371346250,7994,31844,38271,16,1,0,0,13416,100,0,0,5089"));

        verifyPosition(decoder, text(
                "[7051,3041,15270,30179873,710,70618,40335,-94679080,-360604930,0,35454,23148,0,1,0,0,12542,100,13,32,4971"));

    }

}
