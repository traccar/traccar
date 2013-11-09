package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class OsmAndProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        OsmAndProtocolDecoder decoder = new OsmAndProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());
        
        verify(decoder.decode(null, null, new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                "/?id=123456&timestamp=1377177267&lat=60.0&lon=30.0&speed=0.0&bearing=0.0&altitude=0&hdop=0.0")));
        
        verify(decoder.decode(null, null, new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                "/?id=123456&timestamp=1377177267&lat=60.0&lon=30.0")));
        
        verify(decoder.decode(null, null, new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                "/?lat=60.0&lon=30.0&speed=0.0&heading=0.0&vacc=0&hacc=0&altitude=0&deviceid=123456")));

    }

}
