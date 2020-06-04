package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class NiotProtocolDecoderTest extends ProtocolTest {
    
    @Test
    public void testDecode() throws Exception {

        NiotProtocolDecoder decoder = new NiotProtocolDecoder(null);

        verifyPosition(decoder, binary(
                "585880004c08675430355777182005201100468024121b03f390ba00000105f8000b8d207ffc5f0f290084500000000000160001383932353430323130363431363839323430303700050002004e55940d"));

        verifyPosition(decoder, binary(
                "585880004C08640460465310081912101835080011679303C1E18F00400085F8014FBED87FFC4D15290085501A28000000160001383932353430323131313431323931333238343200050002004E55B40D"));

    }

}
