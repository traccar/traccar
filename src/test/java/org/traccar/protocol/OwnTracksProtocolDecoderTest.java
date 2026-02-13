package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class OwnTracksProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new OwnTracksProtocolDecoder(null));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"_type\":\"location\",\"acc\":15,\"alt\":440,\"batt\":46,\"conn\":\"w\",\"lat\":46.0681247,\"lon\":11.1512805,\"t\":\"u\",\"tid\":\"5t\",\"tst\":1551874878,\"vac\":2,\"vel\":0}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"lon\":2.29513,\"lat\":48.85833,\"tst\":1497349316,\"_type\":\"location\",\"tid\":\"JJ\"}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"cog\":271,\"lon\":2.29513,\"acc\":5,\"vel\":61,\"vac\":21,\"lat\":48.85833,\"tst\":1497349316,\"alt\":167,\"_type\":\"location\",\"tid\":\"JJ\",\"t\":\"u\",\"batt\":67}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"lat\":48.85,\"lon\":2.295,\"_type\":\"location\",\"tid\":\"JJ\",\"tst\":1497476456}")));
    }

}
