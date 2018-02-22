package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class GlobeKeeperProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        GlobeKeeperProtocolDecoder decoder = new GlobeKeeperProtocolDecoder(new GlobeKeeperProtocol());

        verifyPosition(decoder, text(
                "48666666666;2017-01-01 16:31:01;52,1133308410645;21,1000003814697;310;120;2292;1"));

        verifyPosition(decoder, text(
                "48666666666;2017-01-01 16:55:00;52,1636123657227;21,0827789306641;0;90;4000;0"));

        verifyPosition(decoder, text(
                "48666666666;2017-01-01 17:32:01;52,1711120605469;21,0866680145264;70;90;1199;0"));

        verifyPosition(decoder, text(
                "48601601601;2017-01-01 16:31:01;52,1133308410645;21,1000003814697;310;360;2292;1"));

        verifyPosition(decoder, text(
                "48601601601;2017-01-01 16:55:00;52,1636123657227;21,0827789306641;0;360;4000;0"));

        verifyPosition(decoder, text(
                "48601601601;2017-01-01 17:32:01;52,1711120605469;21,0866680145264;70;360;1199;0"));

    }

}
