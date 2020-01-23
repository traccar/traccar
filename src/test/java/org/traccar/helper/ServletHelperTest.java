package org.traccar.helper;

import org.apache.struts.mock.MockHttpServletRequest;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ServletHelperTest {

    @Test
    public void testRetrieveRemoteAddressProxyMultiple() {
        MockRequest request = new MockRequest();
        request.setRemoteAddress("147.120.1.5");
        request.addHeader("X-FORWARDED-FOR", "231.23.45.65, 10.20.10.33, 10.20.20.34");

        assertEquals("231.23.45.65", ServletHelper.retrieveRemoteAddress(request));
    }

    @Test
    public void testRetrieveRemoteAddressProxySingle() {
        MockRequest request = new MockRequest();
        request.setRemoteAddress("147.120.1.5");
        request.addHeader("X-FORWARDED-FOR", "231.23.45.65");

        assertEquals("231.23.45.65", ServletHelper.retrieveRemoteAddress(request));
    }

    @Test
    public void testRetrieveRemoteAddressNoProxy() {
        MockRequest request = new MockRequest();
        request.setRemoteAddress("231.23.45.65");

        assertEquals("231.23.45.65", ServletHelper.retrieveRemoteAddress(request));
    }

    private final static class MockRequest extends MockHttpServletRequest {

        private String remoteAddress;

        private Map<String, String> headers = new HashMap<>();

        public void setRemoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
        }

        public void addHeader(String name, String value) {
            headers.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            return headers.get(name);
        }

        @Override
        public String getRemoteAddr() {
            return remoteAddress;
        }

    }

}
