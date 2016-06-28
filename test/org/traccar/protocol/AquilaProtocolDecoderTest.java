package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class AquilaProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        AquilaProtocolDecoder decoder = new AquilaProtocolDecoder(new AquilaProtocol());

        verifyPosition(decoder, text(
                "$$CLIENT_1DT,151028368,1,19.108438,72.925308,160628154920,A,22,0,0,131,3503,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,0,*1D"));

        verifyNothing(decoder, text(
                "$$CLIENT_1DT,160319372,1,28.549541,77.249802,160628140743,A,23,0,-65025,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,*0D"));

        verifyPosition(decoder, text(
                "$$SRINI_1MS,141214807,1,12.963515,77.533844,150925161628,A,27,0,8,0,68,0,0,0,0,0,0,0,0,1,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,*43"));

        verifyPosition(decoder, text(
                "$$CLIENT_1ZF,130329214,1,12.962985,77.576484,140127165433,A,22,0,0,1,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,*26"));

        verifyPosition(decoder, text(
                "$$CLIENT_1WP,141216511,3,12.963123,77.534012,150908163534,A,31,0,0,0,7,0,0,0,0,0,0,0,1,0,1,0,0,1,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,1,1,0,*28"));

        verifyPosition(decoder, text(
                "$$CLIENT_1WP,141216511,3,12.963212,77.533989,150908164041,V,31,0,0,0,8,0,0,0,0,0,0,0,1,0,1,0,0,1,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,1,1,0,*2A"));

    }

}
