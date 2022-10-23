package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class SpotProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new SpotProtocolDecoder(null));

        verifyPositions(decoder, request(HttpMethod.POST, "/", buffer(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n",
                "<messageList xmlns=\"http://v2.shared.globalstar.com\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://v2.shared.globalstar.com http://share.findmespot.com/shared/schema/spotXml-v2.xsd\">\n",
                "<header>\n",
                "<totalCount>1</totalCount>\n",
                "<mode>LIVE</mode>\n",
                "</header>\n",
                "<message>\n",
                "<id>891801957</id>\n",
                "<esn>0-3112123</esn>\n",
                "<esnName>0-3112123a</esnName>\n",
                "<messageType>NEWMOVEMENT</messageType>\n",
                "<messageDetail>SPOT Trace has detected that the asset has moved.</messageDetail>\n",
                "<timestamp>2017-12-27T13:19:38.000Z</timestamp>\n",
                "<timeInGMTSecond>1514380778</timeInGMTSecond>\n",
                "<latitude>-1.28781</latitude>\n",
                "<longitude>-47.93042</longitude>\n",
                "<batteryState>GOOD</batteryState>\n",
                "</message>\n",
                "</messageList>")));

    }

}
