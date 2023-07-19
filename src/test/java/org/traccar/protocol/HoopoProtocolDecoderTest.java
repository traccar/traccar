package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class HoopoProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new HoopoProtocolDecoder(null));

        verifyPosition(decoder, text(
                "{ \"deviceId\": \"BCCD0654\", \"assetName\": \"BCCD0654\", \"assetType\": \"???? ?????? - ??? 8\", \"eventData\": { \"latitude\": 31.97498, \"longitude\": 34.80802, \"locationName\": \"\", \"accuracyLevel\": \"High\", \"eventType\": \"Arrival\", \"batteryLevel\": 100, \"receiveTime\": \"2021-09-20T18:52:32Z\" }, \"eventTime\": \"2021-09-20T08:52:02Z\", \"serverReportTime\": \"0001-01-01T00:00:00Z\" }"));

        verifyPosition(decoder, text(
                "{\"deviceId\":\"BCCD2559\",\"assetName\":\"BCCD2559\",\"assetType\":\"Standard\",\"eventData\":{\"latitude\":32.04746,\"longitude\":34.75843,\"locationName\":\"\",\"accuracyLevel\":\"?????\",\"eventType\":\"InMotion\",\"batteryLevel\":100,\"receiveTime\":\"2022-01-20T08:02:44Z\"},\"address\":{\"line1\":\"????????????70\",\"city\":\"??????-???\",\"stateOrProvince\":\"\",\"postalCode\":\"\",\"countryCode\":\"IL\"},\"movement\":{\"Speed\":1},\"eventTime\":\"2022-01-20T05:55:27Z\",\"serverReportTime\":\"2022-01-20T08:02:48Z\"}"));

    }

}
