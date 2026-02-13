package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class ValtrackProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new ValtrackProtocolDecoder(null));

        verifyPositions(decoder, request(HttpMethod.POST, "/", buffer(
                "{\"resource\":[{\"devid\":\"869731054075783\",\"etype\":\"G_PING\",\"lat\":\"0.000000\",\"lon\":\"0.000000\",\"vbat\":\"12.263848\",\"speed\":\"\",\"nlat\":\"4255.364258\",\"nlon\":\"176.867203\",\"ncsq\":\"16,99\"},{\"devid\":\"869731054075783\",\"etype\":\"G_PING\",\"lat\":\"0.000000\",\"lon\":\"0.000000\",\"vbat\":\"12.263848\",\"speed\":\"\",\"nlat\":\"4255.364258\",\"nlon\":\"176.867203\",\"ncsq\":\"16,99\"},{\"devid\":\"869731054075783\",\"etype\":\"G_PING\",\"lat\":\"0.000000\",\"lon\":\"0.000000\",\"vbat\":\"12.263848\",\"speed\":\"\",\"nlat\":\"4255.364258\",\"nlon\":\"176.867203\",\"ncsq\":\"16,99\"}]}")));

    }

}
