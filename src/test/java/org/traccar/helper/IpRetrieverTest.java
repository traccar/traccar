package org.traccar.helper;


import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;

public class IpRetrieverTest {

    private static final String NORMAL_IP_ADDRESS = "231.23.45.65";
    private static final String GATEWAY_IP_ADDRESS = "147.120.1.5";
    private static final String IP_ADDRESS_BEHIND_REVERSE_PROXY = "231.23.45.65, 10.20.10.33, 10.20.20.34";

    private MockHttpServletRequest mockHttpServletRequest;

    @Before
    public void init() {
        mockHttpServletRequest = new MockHttpServletRequest();
    }

    @Test
    public void testIpBehindReverseProxy() {
        mockHttpServletRequest.setRemoteAddr(GATEWAY_IP_ADDRESS);
        mockHttpServletRequest.addHeader("X-FORWARDED-FOR", IP_ADDRESS_BEHIND_REVERSE_PROXY);

        assertEquals(NORMAL_IP_ADDRESS, IpRetriever.retrieveIP(mockHttpServletRequest));
    }

    @Test
    public void testNormalIp() {
        mockHttpServletRequest.setRemoteAddr(NORMAL_IP_ADDRESS);
        assertEquals(NORMAL_IP_ADDRESS, IpRetriever.retrieveIP(mockHttpServletRequest));

    }

}
