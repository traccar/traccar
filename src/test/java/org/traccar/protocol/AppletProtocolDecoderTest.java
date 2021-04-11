package org.traccar.protocol;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class AppletProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new AppletProtocolDecoder(null);

        DefaultHttpHeaders headers = new DefaultHttpHeaders();

        headers.add("HOST", "192.168.0.1:8080");
        headers.add("X-Admin-Protocol", "globalplatform-remote-admin/1.0");
        headers.add("X-Admin-From", "8943170080001406197F");
        headers.add("User-Agent", "oma-scws-admin-agent/1.1");
        headers.add("From", "8943170080001406197F");

        verifyNull(decoder, request(HttpMethod.POST, "/pli?=", headers));

    }

}
