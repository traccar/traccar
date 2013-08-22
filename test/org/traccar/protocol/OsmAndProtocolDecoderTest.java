package org.traccar.protocol;

import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class OsmAndProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        OsmAndProtocolDecoder decoder = new OsmAndProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());
        
        assertNotNull(decoder.decode(null, null, new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                "/?id=123456&timestamp=1377177267&lat=60.0&lon=30.0&speed=0.0&bearing=0.0&altitude=0&hdop=0.0")));
        
        assertNotNull(decoder.decode(null, null, new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                "/?id=123456&timestamp=1377177267&lat=60.0&lon=30.0")));

    }

}
