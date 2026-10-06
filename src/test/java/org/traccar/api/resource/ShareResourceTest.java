package org.traccar.api.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traccar.api.BaseResource;
import org.traccar.api.security.PermissionsService;
import org.traccar.api.security.UserPrincipal;
import org.traccar.api.signature.TokenManager;
import org.traccar.config.Config;
import org.traccar.model.Device;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Request;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.lang.reflect.Field;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShareResourceTest {

    private Storage storage;
    private TokenManager tokenManager;
    private ShareResource resource;

    private static void inject(Class<?> clazz, Object target, String name, Object value) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @BeforeEach
    public void setUp() throws Exception {
        User user = new User();
        user.setId(1);
        user.setEmail("user@example.com");

        Device device = new Device();
        device.setId(2);
        device.setName("device");
        device.setUniqueId("123456");

        storage = mock(Storage.class);
        when(storage.getObject(eq(Device.class), any(Request.class))).thenReturn(device);
        when(storage.addObject(any(User.class), any(Request.class))).thenReturn(3L);

        PermissionsService permissionsService = mock(PermissionsService.class);
        when(permissionsService.getServer()).thenReturn(new Server());
        when(permissionsService.getUser(anyLong())).thenReturn(user);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(new UserPrincipal(user.getId(), null));

        tokenManager = mock(TokenManager.class);
        when(tokenManager.generateToken(anyLong(), any())).thenReturn("token");

        resource = new ShareResource();
        inject(BaseResource.class, resource, "storage", storage);
        inject(BaseResource.class, resource, "permissionsService", permissionsService);
        inject(BaseResource.class, resource, "securityContext", securityContext);
        inject(ShareResource.class, resource, "config", new Config());
        inject(ShareResource.class, resource, "tokenManager", tokenManager);
    }

    @Test
    public void testShareDevice() throws Exception {
        Date expiration = new Date(System.currentTimeMillis() + 3600000);

        assertEquals("token", resource.shareDevice(2, expiration));
        verify(storage).addObject(any(User.class), any(Request.class));
        verify(tokenManager).generateToken(3, expiration);
    }

    @Test
    public void testShareDeviceAgainAfterExpiration() throws Exception {
        User share = new User();
        share.setId(3);
        share.setEmail("user@example.com:123456");
        share.setTemporary(true);
        share.setExpirationTime(new Date(System.currentTimeMillis() - 3600000));
        when(storage.getObject(eq(User.class), any(Request.class))).thenReturn(share);

        Date expiration = new Date(System.currentTimeMillis() + 3600000);
        WebApplicationException exception = assertThrows(
                WebApplicationException.class, () -> resource.shareDevice(2, expiration));

        assertEquals(Response.Status.CONFLICT.getStatusCode(), exception.getResponse().getStatus());
        verify(storage, never()).addObject(any(User.class), any(Request.class));
        verify(tokenManager, never()).generateToken(anyLong(), any());
    }

}
