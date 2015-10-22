package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

public class MiniFinderProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        MiniFinderProtocolDecoder decoder = new MiniFinderProtocolDecoder(new MiniFinderProtocol());

        verifyNothing(decoder, text( "!1,860719020212696"));

        verifyPosition(decoder, text(
                "!D,22/2/14,13:40:58,56.899601,14.811541,0,0,1,176.0,98,5,16,0"));

        verifyPosition(decoder, text(
                "!D,22/2/14,13:47:51,56.899517,14.811665,0,0,b0001,179.3,97,5,16,0"));

        verifyPosition(decoder, text(
                "!D,3/7/13,6:35:30,22.645952,114.040436,0.0,225.8,1f0001,12.11,98,0,0,0"));

    }

}
