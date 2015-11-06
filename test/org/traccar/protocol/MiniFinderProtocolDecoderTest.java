package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

public class MiniFinderProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        MiniFinderProtocolDecoder decoder = new MiniFinderProtocolDecoder(new MiniFinderProtocol());

        verifyNothing(decoder, text(
                "!1,123456789012345"));

        verifyNothing(decoder, text(
                "!5,17,V"));

        verifyNothing(decoder, text(
                "!1,860719027585011"));

        verifyPosition(decoder, text(
                "!A,26/10/12,00:28:41,7.770385,-72.215706,0.0,25101,0"));

        verifyPosition(decoder, text(
                "!A,01/12/10,13:25:35,22.641724,114.023666,000.1,281.6,0"));

        verifyPosition(decoder, text(
                "!D,08/07/15,04:01:32,40.428257,-3.704808,0,0,170001,701.7,22,5,14,0"));

        verifyPosition(decoder, text(
                "!D,08/07/15,04:55:13,40.428257,-3.704932,0,0,180001,680.0,8,8,13,0"));

        verifyPosition(decoder, text(
                "!D,08/07/15,02:01:32,40.428230,-3.704950,4,170,170001,682.7,43,6,13,0"));

        verifyNothing(decoder, text(
                "!1,860719020212696"));

        verifyPosition(decoder, text(
                "!D,22/2/14,13:40:58,56.899601,14.811541,0,0,1,176.0,98,5,16,0"),
                position("2014-02-22 13:40:58.000", true, 56.89960, 14.81154));

        verifyPosition(decoder, text(
                "!D,22/2/14,13:47:51,56.899517,14.811665,0,0,b0001,179.3,97,5,16,0"));

        verifyPosition(decoder, text(
                "!D,3/7/13,6:35:30,22.645952,114.040436,0.0,225.8,1f0001,12.11,98,0,0,0"));

    }

}
