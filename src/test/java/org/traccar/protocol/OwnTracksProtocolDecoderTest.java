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

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"lon\":-122,\"acc\":5,\"created_at\":1736161415,\"lat\":37,\"topic\":\"owntracks/ckrey/22A67880-15C3-41A6-9157-25545C7993AC\",\"t\":\"u\",\"m\":1,\"tst\":1736161169,\"conn\":\"w\",\"alt\":0,\"_type\":\"location\",\"tid\":\"AC\"}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"topic\":\"owntracks/qtripp/865284041968706\",\"lat\":47.420051,\"lon\":13.654704,\"vel\":0,\"cog\":113,\"tst\":1736519167,\"mcc\":232,\"mnc\":1,\"lac\":\"4F4D\",\"cid\":\"673A\",\"_type\":\"location\",\"acc\":10,\"alt\":1113,\"t\":\"I\",\"don\":105,\"tid\":\"CS\"}")));
    }

}
