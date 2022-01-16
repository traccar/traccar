package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class OrbcommProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new OrbcommProtocolDecoder(null);

        verifyNull(decoder, response(
                buffer("{\"ErrorID\":0,\"NextStartUTC\":\"2016-10-13 15:19:59\",\"Messages\":[{\"ID\":120213064,\"MessageUTC\":\"2016-10-12 12:42:01\",\"ReceiveUTC\":\"2016-10-12 12:42:01\",\"SIN\":0,\"MobileID\":\"01173096SKY0E45\",\"Payload\":{\"Name\":\"modemRegistration\",\"SIN\":0,\"MIN\":0,\"Fields\":[{\"Name\":\"hardwareMajorVersion\",\"Value\":\"4\"},{\"Name\":\"hardwareMinorVersion\",\"Value\":\"2\"},{\"Name\":\"softwareMajorVersion\",\"Value\":\"13\"},{\"Name\":\"softwareMinorVersion\",\"Value\":\"1\"},{\"Name\":\"product\",\"Value\":\"4\"},{\"Name\":\"wakeupPeriod\",\"Value\":\"None\"},{\"Name\":\"lastResetReason\",\"Value\":\"Software\"},{\"Name\":\"virtualCarrier\",\"Value\":\"6\"},{\"Name\":\"beam\",\"Value\":\"1\"},{\"Name\":\"vain\",\"Value\":\"0\"},{\"Name\":\"reserved\",\"Value\":\"0\"},{\"Name\":\"operatorTxState\",\"Value\":\"0\"},{\"Name\":\"userTxState\",\"Value\":\"0\"},{\"Name\":\"broadcastIDCount\",\"Value\":\"0\"}}],\"RegionName\":\"AMERRB11\",\"OTAMessageSize\":15,\"CustomerID\":0}]}")));

    }

}
