package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class ObdDongleProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new ObdDongleProtocolDecoder(null));

        verifyNull(decoder, binary(
                "55550003383634383637303232353131303135010009010011023402010201ABAAAA"));

        verifyPosition(decoder, binary(
                "5555000338363438363730323235313130313503000100010355AABBCC184F1ABC614E21C1FA08712A84ABAAAA"),
                position("2015-07-18 20:49:16.000", true, 22.12346, -123.45678));

    }

}
