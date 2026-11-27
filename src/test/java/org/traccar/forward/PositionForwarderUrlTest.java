package org.traccar.forward;

import org.junit.jupiter.api.Test;
import org.traccar.BaseTest;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Device;
import org.traccar.model.Position;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PositionForwarderUrlTest extends BaseTest {

    @Test
    public void testFormatRequest() throws Exception {

        Config config = new Config();
        config.setString(Keys.FORWARD_URL, "http://localhost/?fixTime={fixTime}&gprmc={gprmc}&name={name}");

        Position position = new Position();
        position.setTime(Date.from(Instant.parse("2016-01-01T01:02:03Z")));
        position.setValid(true);
        position.setLatitude(20);
        position.setLongitude(30);

        var device = mock(Device.class);
        when(device.getId()).thenReturn(1L);
        when(device.getName()).thenReturn("test");
        when(device.getUniqueId()).thenReturn("123456789012345");
        when(device.getStatus()).thenReturn(Device.STATUS_ONLINE);

        PositionData positionData = new PositionData();
        positionData.setPosition(position);
        positionData.setDevice(device);

        PositionForwarderUrl forwarder = new PositionForwarderUrl(config, null, null);

        assertEquals(
                "http://localhost/?fixTime=1451610123000&gprmc=$GPRMC,010203.000,A,2000.0000,N,03000.0000,E,0.00,0.00,010116,,*05&name=test",
                forwarder.formatRequest(positionData));

    }

}
