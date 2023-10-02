package org.traccar.helper;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebHelperTest {

    @Test
    public void testRetrieveRemoteAddressProxyMultiple() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("147.120.1.5");
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("231.23.45.65, 10.20.10.33, 10.20.20.34");

        assertEquals("231.23.45.65", WebHelper.retrieveRemoteAddress(request));
    }

    @Test
    public void testRetrieveRemoteAddressProxySingle() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("147.120.1.5");
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("231.23.45.65");

        assertEquals("231.23.45.65", WebHelper.retrieveRemoteAddress(request));
    }

    @Test
    public void testRetrieveRemoteAddressNoProxy() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("231.23.45.65");

        assertEquals("231.23.45.65", WebHelper.retrieveRemoteAddress(request));
    }

}
