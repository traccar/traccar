package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class GlobalstarProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        GlobalstarProtocolDecoder decoder = new GlobalstarProtocolDecoder(null);

        verifyPositions(decoder, request(HttpMethod.POST, "/", buffer(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<stuMessages xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://cody.glpconnect.com/XSD/StuMessage_Rev1_0_1.xsd\" timeStamp=\"17/02/2019 21:56:15 GMT\" messageID=\"2a471778dda31005850dc52bb93ae81a\">",
                "<stuMessage>",
                "<esn>0-2654816</esn>",
                "<unixTime>1550440592</unixTime>",
                "<gps>N</gps>",
                "<payload length=\"9\" source=\"pc\" encoding=\"hex\">0x00337BA619B7250A00</payload>",
                "</stuMessage>",
                "</stuMessages>")));

    }

}
