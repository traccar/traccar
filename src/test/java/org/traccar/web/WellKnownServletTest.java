package org.traccar.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.traccar.config.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WellKnownServletTest {

    private final WellKnownServlet servlet = new WellKnownServlet(new Config(), new ObjectMapper());

    @Test
    public void testBarePathUnchanged() {
        assertEquals("/openid-configuration", servlet.normalizePath("/openid-configuration"));
        assertEquals("/oauth-authorization-server", servlet.normalizePath("/oauth-authorization-server"));
        assertEquals("/oauth-protected-resource", servlet.normalizePath("/oauth-protected-resource"));
    }

    @Test
    public void testSuffixedPathNormalized() {
        assertEquals("/openid-configuration", servlet.normalizePath("/openid-configuration/api/oidc"));
        assertEquals("/oauth-authorization-server", servlet.normalizePath("/oauth-authorization-server/api/oidc"));
        assertEquals("/oauth-protected-resource", servlet.normalizePath("/oauth-protected-resource/api/mcp"));
    }

    @Test
    public void testWrongSuffixNotNormalized() {
        assertEquals("/oauth-protected-resource/api/oidc", servlet.normalizePath("/oauth-protected-resource/api/oidc"));
        assertEquals("/openid-configuration/api/mcp", servlet.normalizePath("/openid-configuration/api/mcp"));
    }

    @Test
    public void testUnknownPathUnchanged() {
        assertEquals("/unknown-path", servlet.normalizePath("/unknown-path"));
    }

    @Test
    public void testNullPath() {
        assertEquals("", servlet.normalizePath(null));
    }

}
