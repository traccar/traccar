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

public class GpxExportProviderTest {

    @Test
    public void testGenerateOutput() throws Exception {
        Storage storage = mock(Storage.class);

        Device device = new Device();
        device.setName("A & B <test>");
        when(storage.getObject(eq(Device.class), any())).thenReturn(device);

        Position position = new Position();
        position.setFixTime(new Date(0));
        position.setLatitude(10.5);
        position.setLongitude(20.25);
        position.setAltitude(30.75);
        when(storage.getObjectsStream(eq(Position.class), any())).thenReturn(Stream.of(position));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        try {
            new GpxExportProvider(storage).generate(outputStream, 1, new Date(0), new Date(60_000));
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }

        String result = outputStream.toString(StandardCharsets.UTF_8);
        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<gpx version=\"1.0\">"
                + "<trk>"
                + "<name>A &amp; B &lt;test></name>"
                + "<trkseg>"
                + "<trkpt lat=\"10.5\" lon=\"20.25\">"
                + "<ele>30.75</ele>"
                + "<time>1970-01-01T00:00:00Z</time>"
                + "</trkpt>"
                + "</trkseg>"
                + "</trk>"
                + "</gpx>";

        assertEquals(expected, result);
    }
}
