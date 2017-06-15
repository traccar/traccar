package org.traccar.protocol;

import org.jboss.netty.handler.codec.http.HttpMethod;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class OwnTracksProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        OwnTracksProtocolDecoder decoder = new OwnTracksProtocolDecoder(new OwnTracksProtocol());

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"lon\":2.29513,\"lat\":48.85833,\"tst\":1497349316,\"_type\":\"location\",\"tid\":\"JJ\"}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"cog\":271,\"lon\":2.29513,\"acc\":5,\"vel\":61,\"vac\":21,\"lat\":48.85833,\"tst\":1497349316,\"alt\":167,\"_type\":\"location\",\"tid\":\"JJ\",\"t\":\"u\",\"batt\":67}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"lat\":48.85,\"lon\":2.295,\"_type\":\"location\",\"tid\":\"JJ\",\"tst\":1497476456}")));
    }

}
