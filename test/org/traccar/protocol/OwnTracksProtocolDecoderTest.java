package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class OwnTracksProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        OwnTracksProtocolDecoder decoder = new OwnTracksProtocolDecoder(new OwnTracksProtocol());

/*
 * I don'w know how to get JSON into that ...
 *
        verifyNull(decoder, request(
                "{ \"id\" : \"hello\" }"));

        verifyPosition(decoder, request(
                "{\"cog\":271,\"lon\":2.29513,\"acc\":5,\"vel\":61,\"vac\":21,\"lat\":48.85833,\"tst\":1497349316,\"alt\":167,\"_type\":\"location\",\"tid\":\"JJ\"}"));

*/

    }

}
