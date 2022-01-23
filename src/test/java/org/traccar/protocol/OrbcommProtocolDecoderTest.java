package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class OrbcommProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new OrbcommProtocolDecoder(null);

        verifyPositions(decoder, response(
                buffer("{\"ErrorID\":0,\"NextStartUTC\":\"2022-01-20 21:17:19\",\"Messages\":[{\"ID\":21545955455454,\"MessageUTC\":\"2022-01-20 21:17:19\",\"ReceiveUTC\":\"2022-01-20 21:17:19\",\"SIN\":19,\"MobileID\":\"01097623SKY2C68\",\"Payload\":{\"Name\":\"simpleReport\",\"SIN\":19,\"MIN\":1,\"Fields\":[{\"Name\":\"latitude\",\"Value\":\"2717900\",\"Type\":\"signedint\"},{\"Name\":\"longitude\",\"Value\":\"-4555211\",\"Type\":\"signedint\"},{\"Name\":\"speed\",\"Value\":\"0\",\"Type\":\"unsignedint\"},{\"Name\":\"heading\",\"Value\":\"1439\",\"Type\":\"unsignedint\"}]},\"RegionName\":\"\",\"OTAMessageSize\":17,\"CustomerID\":0,\"Transport\":1,\"MobileOwnerID\":60000934}]}")));

        verifyPositions(decoder, false, response(
                buffer("{\"ErrorID\":0,\"NextStartUTC\":\"2016-10-13 15:19:59\",\"Messages\":[{\"ID\":120213064,\"MessageUTC\":\"2016-10-12 12:42:01\",\"ReceiveUTC\":\"2016-10-12 12:42:01\",\"SIN\":0,\"MobileID\":\"01173096SKY0E45\",\"Payload\":{\"Name\":\"modemRegistration\",\"SIN\":0,\"MIN\":0,\"Fields\":[{\"Name\":\"hardwareMajorVersion\",\"Value\":\"4\"},{\"Name\":\"hardwareMinorVersion\",\"Value\":\"2\"},{\"Name\":\"softwareMajorVersion\",\"Value\":\"13\"},{\"Name\":\"softwareMinorVersion\",\"Value\":\"1\"},{\"Name\":\"product\",\"Value\":\"4\"},{\"Name\":\"wakeupPeriod\",\"Value\":\"None\"},{\"Name\":\"lastResetReason\",\"Value\":\"Software\"},{\"Name\":\"virtualCarrier\",\"Value\":\"6\"},{\"Name\":\"beam\",\"Value\":\"1\"},{\"Name\":\"vain\",\"Value\":\"0\"},{\"Name\":\"reserved\",\"Value\":\"0\"},{\"Name\":\"operatorTxState\",\"Value\":\"0\"},{\"Name\":\"userTxState\",\"Value\":\"0\"},{\"Name\":\"broadcastIDCount\",\"Value\":\"0\"}],\"RegionName\":\"AMERRB11\",\"OTAMessageSize\":15,\"CustomerID\":0}}]}")));

    }

}
