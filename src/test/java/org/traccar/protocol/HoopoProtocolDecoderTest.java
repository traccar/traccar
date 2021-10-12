package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class HoopoProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new HoopoProtocolDecoder(null);

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{ \"deviceId\": \"BCCD0654\", \"assetName\": \"BCCD0654\", \"assetType\": \"???? ?????? - ??? 8\", \"eventData\": { \"latitude\": 31.97498, \"longitude\": 34.80802, \"locationName\": \"\", \"accuracyLevel\": \"High\", \"eventType\": \"Arrival\", \"batteryLevel\": 100, \"receiveTime\": \"2021-09-20T18:52:32Z\" }, \"eventTime\": \"2021-09-20T08:52:02Z\", \"serverReportTime\": \"0001-01-01T00:00:00Z\" }")));

    }

}
