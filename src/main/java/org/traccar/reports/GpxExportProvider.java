/*
 * Copyright 2022 - 2026 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.reports;

import org.traccar.helper.DateUtil;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.model.Geofence;
import org.traccar.model.Position;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Stream;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class GpxExportProvider {

    private final Storage storage;

    @Inject
    public GpxExportProvider(Storage storage) {
        this.storage = storage;
    }

    public void generate(
            OutputStream outputStream, long deviceId, long geofenceId, Date from, Date to)
            throws StorageException, XMLStreamException {

        var device = storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("id", deviceId)));

        Geofence geofence = geofenceId == 0 ? null : storage.getObject(Geofence.class, new Request(
                new Columns.All(), new Condition.Equals("id", geofenceId)));

        XMLStreamWriter writer = XMLOutputFactory.newFactory()
                .createXMLStreamWriter(outputStream, StandardCharsets.UTF_8.name());

        writer.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
        writer.writeStartElement("gpx");
        writer.writeAttribute("version", "1.0");
        writer.writeStartElement("trk");
        writer.writeStartElement("name");
        writer.writeCharacters(device.getName());
        writer.writeEndElement();
        writer.writeStartElement("trkseg");
        try (Stream<Position> positions = PositionUtil.getPositionsStream(storage, deviceId, from, to)
                .filter(position -> geofence == null || geofence.containsPosition(position))) {
            for (var iterator = positions.iterator(); iterator.hasNext();) {
                Position position = iterator.next();
                writer.writeStartElement("trkpt");
                writer.writeAttribute("lat", Double.toString(position.getLatitude()));
                writer.writeAttribute("lon", Double.toString(position.getLongitude()));
                writer.writeStartElement("ele");
                writer.writeCharacters(Double.toString(position.getAltitude()));
                writer.writeEndElement();
                writer.writeStartElement("time");
                writer.writeCharacters(DateUtil.formatDate(position.getFixTime()));
                writer.writeEndElement();
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

}
