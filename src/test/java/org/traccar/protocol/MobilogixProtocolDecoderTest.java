package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class MobilogixProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        MobilogixProtocolDecoder decoder = new MobilogixProtocolDecoder(null);

        verifyNull(decoder, text(
                "[2020-09-25 21:21:43,T1,1,V1.1.1,201951132415,,,12345678,724108034109376,359366080215256"));

        verifyPosition(decoder, text(
                "[2011-12-15 10:00:00,T2,1,V1.0.0,A123045612AA123488,1B,4.5,22.564152,113.252432,50.6,270.5,1,460:00:10101:03633"));

        verifyPosition(decoder, text(
                "[2011-12-15 10:00:00,T3,1,V1.0.0,A123045612AA123488,1B,4.5,22.564152,113.252432,50.6,270.5,1"));

    }

}
