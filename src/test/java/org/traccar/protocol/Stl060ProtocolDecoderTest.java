package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Stl060ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new Stl060ProtocolDecoder(null);

        verifyPosition(decoder, text(
                "$1,357804048043099,D001,AP29AW0963,23/02/14,14:06:54,17248488N,078342226E,0.08,193.12,1,1,1,1,1,A"),
                position("2014-02-23 14:06:54.000", true, 17.41415, 78.57038));

        verifyPosition(decoder, text(
                "$1,357804048043099,D001,AP29AW0963,12/05/14,07:39:57,1724.8564N,07834.2199E,0.00,302.84,1,1,1,1,1,A"));
        
        verifyPosition(decoder, text(
                "$1,357804047969310,D001,AP29AW0963,01/01/13,13:24:47,1723.9582N,07834.0945E,00100,010,0,0,0,0,0,A,"));

        verifyPosition(decoder, text(
                "$1,357804047969310,D001,AP29AW0963,01/01/13,13:24:47,1723.9582N,07834.0945E,00100,010,0,0,0,0,0,0008478660,1450,40,34,0,0,0,A"));

    }

}
