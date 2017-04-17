package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class TrvProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        TrvProtocolDecoder decoder = new TrvProtocolDecoder(new TrvProtocol());

        verifyNull(decoder, text(
                "TRVAP00353456789012345"));

        verifyAttributes(decoder, text(
                "TRVCP01,06000908000102"));

        verifyAttributes(decoder, text(
                "TRVCP01,100007100000001020151060011"));

        verifyPosition(decoder, text(
                "TRVAP01160211A2544.5118N05553.7586E105.711185941.52010001010010000,424,030,3011,27003"));

        verifyPosition(decoder, text(
                "TRVAP01160209A2540.8863N05546.6125E005.6075734123.7910000810010000,424,030,3012,27323"));

        verifyPosition(decoder, text(
                "TRVAP01080524A2232.9806N11404.9355E000.1061830323.8706000908000102,460,0,9520,3671"));

        verifyPosition(decoder, text(
                "TRVAP01080524A2232.9806N11404.9355E000.1061830323.8706000908000102,460,0,9520,3671"),
                position("2008-05-24 06:18:30.000", true, 22.54968, 114.08226));

        verifyPosition(decoder, text(
                "TRVAP10080524A2232.9806N11404.9355E000.1061830323.8706000908000502,460,0,9520,3671,00,zh-cn,00"));

    }

}
