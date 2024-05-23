package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class GlobalstarProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new GlobalstarProtocolDecoder(null));

        decoder.setModelOverride("AtlasTrax");

        verifyNull(decoder, request(HttpMethod.POST, "/", buffer(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n",
                "<stuMessages xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://cody.glpconnect.com/XSD/StuMessage_Rev1_0_1.xsd\" timeStamp=\"16/09/2020 01:33:07 GMT\" messageID=\"567207180ae9100687cef8c81978371a\">\n",
                "<stuMessage>\n",
                "<esn>0-4325340</esn>\n",
                "<unixTime>1600220003</unixTime>\n",
                "<gps>N</gps>\n",
                "<payload length=\"9\" source=\"pc\" encoding=\"hex\">0x63FFFF1BB4FFFFFFFF</payload>\n",
                "</stuMessage>\n",
                "</stuMessages>")));

        decoder.setModelOverride(null);

        verifyPositions(decoder, request(HttpMethod.POST, "/", buffer(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<stuMessages xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://cody.glpconnect.com/XSD/StuMessage_Rev1_0_1.xsd\" timeStamp=\"25/03/2020 03:02:32 GMT\" messageID=\"300421a0fd2a100585bdde409d6f601a\">",
                "<stuMessage>",
                "<esn>0-2682225</esn>",
                "<unixTime>1585105370</unixTime>",
                "<gps>N</gps>",
                "<payload length=\"9\" source=\"pc\" encoding=\"hex\">0x00C583EACD37210A00</payload>",
                "</stuMessage>",
                "</stuMessages>")));

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
