package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class OsmAndProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        OsmAndProtocolDecoder decoder = new OsmAndProtocolDecoder(new OsmAndProtocol());

        verifyNull(decoder, request(
                "/?timestamp=1377177267&lat=60.0&lon=30.0"));

        verifyPosition(decoder, request(
                "/?id=902064&lat=42.06288&lon=-88.23412&timestamp=2016-01-27T18%3A55%3A47Z&hdop=6.0&altitude=224.0&speed=0.0"));

        verifyPosition(decoder, request(
                "/?id=902064&lat=42.06288&lon=-88.23412&timestamp=1442068686579&hdop=6.0&altitude=224.0&speed=0.0"));

        verifyPosition(decoder, request(
                "/?lat=49.60688&lon=6.15788&timestamp=2014-06-04+09%3A10%3A11&altitude=384.7&speed=0.0&id=353861053849681"));
        
        verifyPosition(decoder, request(
                "/?id=123456&timestamp=1377177267&lat=60.0&lon=30.0&speed=0.0&bearing=0.0&altitude=0&hdop=0.0"));
        
        verifyPosition(decoder, request(
                "/?id=123456&timestamp=1377177267&lat=60.0&lon=30.0"));
        
        verifyPosition(decoder, request(
                "/?lat=60.0&lon=30.0&speed=0.0&heading=0.0&vacc=0&hacc=0&altitude=0&deviceid=123456"));

        verifyPosition(decoder, request(
                "/?id=861001000719969&lat=41.666667&lon=-0.883333&altitude=350.059479&speed=0.000000&batt=87"));

        verifyPosition(decoder, request(
                "/?id=123456&timestamp=1377177267&location=60.0,30.0"));

    }

}
