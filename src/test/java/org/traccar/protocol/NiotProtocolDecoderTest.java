package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class NiotProtocolDecoderTest extends ProtocolTest {
    
    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new NiotProtocolDecoder(null));

        verifyPosition(decoder, binary(
                "585880004c08675430347318522007161451458024b28003f566ee00000328f8000748217ffc500729007a280000000000160001383932353430323130363431363738373136323100050002004e00570d"),
                position("2020-07-16 14:51:45.000", true, -1.33611, 36.89684));

        verifyPosition(decoder, binary(
                "585880004c08675430355777182005201100468024121b03f390ba00000105f8000b8d207ffc5f0f290084500000000000160001383932353430323130363431363839323430303700050002004e55940d"));

        verifyPosition(decoder, binary(
                "585880004C08640460465310081912101835080011679303C1E18F00400085F8014FBED87FFC4D15290085501A28000000160001383932353430323131313431323931333238343200050002004E55B40D"));

    }

}
