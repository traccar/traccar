package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

public class HuabaoProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        HuabaoProtocolDecoder decoder = new HuabaoProtocolDecoder(new HuabaoProtocol());

        verifyNothing(decoder, binary(
                "7E0100002D013511221122000500000000373031303748422D52303347424400000000000000000000003233363631303402CBD5424136383630387E"));

        verifyNothing(decoder, binary(
                "7e0100002d007089994489002800000000000000000048422d523033474244000000000000000000000031393036373531024142433030303030d17e"));

        verifyNothing(decoder, binary(
                "7E0102000E013511221122000661757468656E7469636174696F6E3F7E"));

        verifyPosition(decoder, binary(
                "7E02000032013511221122000700000000000C000301578CC006CA3A5C00470000000014072317201501040000000030011631010BD07E"));

    }

}
