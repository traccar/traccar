package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class StbProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new StbProtocolDecoder(null);

        verifyNull(decoder, text(
                "{\"devId\":\"CHZD08KPD0210425046\",\"devType\":2,\"hardVersion\":\"HDTTVA19\",\"msgType\":110,\"protocolVersion\":\"V1\",\"softVersion\":\"3.1.8\",\"switchCabStatus\":\"1\",\"txnNo\":\"1625212741537\"}"));

    }

}
