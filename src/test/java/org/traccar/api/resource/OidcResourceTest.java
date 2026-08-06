package org.traccar.api.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OidcResourceTest {

    @Test
    public void testValidEntry() {
        assertArrayEquals(
                new String[] {"client", "secret", "https://example.com/callback"},
                OidcResource.parseClientEntry("client:secret:https://example.com/callback"));
    }

    @Test
    public void testValidEntryWithMultipleRedirectUris() {
        assertArrayEquals(
                new String[] {"client", "secret", "https://a.com|https://b.com"},
                OidcResource.parseClientEntry("client:secret:https://a.com|https://b.com"));
    }

    @Test
    public void testMissingRedirectUri() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> OidcResource.parseClientEntry("client:secret"));
        assertTrue(exception.getMessage().contains("client:secret"));
        assertTrue(exception.getMessage().contains("clientId:clientSecret:redirectUri"));
    }

    @Test
    public void testBlankField() {
        assertThrows(IllegalArgumentException.class,
                () -> OidcResource.parseClientEntry("client::https://example.com/callback"));
    }

}
