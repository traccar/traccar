package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class NetProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        NetProtocolDecoder decoder = new NetProtocolDecoder(null);

        verifyPosition(decoder, text(
                "@L03686090604017761712271020161807037078881037233751000000010F850036980A4000"));

        verifyPosition(decoder, text(
                "@L0368609060401776171223102005072803703296103721462100008009000000300B12B000"));

    }

}
