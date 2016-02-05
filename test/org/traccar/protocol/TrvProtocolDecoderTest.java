package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class TrvProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        TrvProtocolDecoder decoder = new TrvProtocolDecoder(new TrvProtocol());

        verifyNothing(decoder, text(
                "TRVAP00353456789012345"));

        verifyAttributes(decoder, text(
                "TRVCP01,06000908000102"));

        verifyPosition(decoder, text(
                "TRVAP01080524A2232.9806N11404.9355E000.1061830323.8706000908000102,460,0,9520,3671"));

        verifyPosition(decoder, text(
                "TRVAP01080524A2232.9806N11404.9355E000.1061830323.8706000908000102,460,0,9520,3671"),
                position("2008-05-24 06:18:30.000", true, 22.54968, 114.08226));

        verifyPosition(decoder, text(
                "TRVAP10080524A2232.9806N11404.9355E000.1061830323.8706000908000502,460,0,9520,3671,00,zh-cn,00"));

    }

}
