package org.traccar.reports;

import org.junit.jupiter.api.Test;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.storage.Storage;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KmlExportProviderTest {

    @Test
    public void testGenerateOutput() throws Exception {
        Storage storage = mock(Storage.class);

        Device device = new Device();
        device.setName("A & B <test>");
        when(storage.getObject(eq(Device.class), any())).thenReturn(device);

        Position position = new Position();
        position.setLatitude(10.5);
        position.setLongitude(20.25);
        position.setAltitude(30.75);
        when(storage.getObjectsStream(eq(Position.class), any())).thenReturn(Stream.of(position));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Date from = new Date(0);
        Date to = new Date(60_000);
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        try {
            new KmlExportProvider(storage).generate(outputStream, 1, from, to);
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }

        String result = outputStream.toString(StandardCharsets.UTF_8);
        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<kml xmlns=\"http://www.opengis.net/kml/2.2\">"
                + "<Document>"
                + "<name>A &amp; B &lt;test></name>"
                + "<Placemark>"
                + "<name>1970-01-01 00:00 - 1970-01-01 00:01</name>"
                + "<LineString>"
                + "<extrude>1</extrude>"
                + "<tessellate>1</tessellate>"
                + "<altitudeMode>absolute</altitudeMode>"
                + "<coordinates>20.250000,10.500000,30.750000</coordinates>"
                + "</LineString>"
                + "</Placemark>"
                + "</Document>"
                + "</kml>";

        assertEquals(expected, result);
    }
}
