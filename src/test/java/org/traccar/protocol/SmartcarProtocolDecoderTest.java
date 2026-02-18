package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class SmartcarProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new SmartcarProtocolDecoder(null));

        verifyPosition(decoder, request(HttpMethod.POST, "/", buffer(
                "{\"version\":\"4.0\",\"eventId\":\"f4f5f122-26bc-458f-b5d8-c7782f49f1c7\",\"eventType\":\"VEHICLE_STATE\",\"meta\":{\"deliveredAt\":1731940433067},\"vehicleId\":\"9af1324f-c44f-4846-a65b-f72fce04f929\",\"data\":{\"user\":{\"id\":\"265f5f9e-f20f-43f6-8ca9-a177b049f43e\"},\"vehicle\":{\"id\":\"9af1324f-c44f-4846-a65b-f72fce04f929\"},\"triggers\":[{\"id\":\"2f9e57f9-9f8c-4f4e-bf94-27f6f6f71d2a\",\"version\":\"v2\"}],\"signals\":[{\"code\":\"location-preciselocation\",\"body\":{\"latitude\":37.4292,\"longitude\":-122.1381},\"meta\":{\"fetchedAt\":1731940328000,\"oemUpdatedAt\":1731940330000}},{\"code\":\"tractionbattery-stateofcharge\",\"body\":{\"value\":78,\"unit\":\"percent\"},\"meta\":{\"fetchedAt\":1731940328000,\"oemUpdatedAt\":1731940330000}},{\"code\":\"charge-voltage\",\"body\":{\"value\":240,\"unit\":\"volt\"},\"meta\":{\"fetchedAt\":1731940328000,\"oemUpdatedAt\":1731940330000}}]}}")));
    }

}
