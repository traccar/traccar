package org.traccar;

import org.junit.Test;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebDataHandlerTest extends ProtocolTest {

    @Test
    public void testFormatRequest() throws Exception {

        Config config = new Config();
        config.setString(Keys.FORWARD_URL, "http://localhost/?fixTime={fixTime}&gprmc={gprmc}&name={name}");

        Position position = position("2016-01-01 01:02:03.000", true, 20, 30);

        var device = mock(Device.class);
        when(device.getId()).thenReturn(1L);
        when(device.getName()).thenReturn("test");
        when(device.getUniqueId()).thenReturn("123456789012345");
        when(device.getStatus()).thenReturn(Device.STATUS_ONLINE);
        var cacheManager = mock(CacheManager.class);
        when(cacheManager.getObject(eq(Device.class), anyLong())).thenReturn(device);

        WebDataHandler handler = new WebDataHandler(config, cacheManager, null, null, null);

        assertEquals(
                "http://localhost/?fixTime=1451610123000&gprmc=$GPRMC,010203.000,A,2000.0000,N,03000.0000,E,0.00,0.00,010116,,*05&name=test",
                handler.formatRequest(position));

    }

}
