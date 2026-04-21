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

import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class KmlExportProvider {

    private final Storage storage;

    @Inject
    public KmlExportProvider(Storage storage) {
        this.storage = storage;
    }

    public void generate(
            OutputStream outputStream, long deviceId, Date from, Date to)
            throws StorageException, XMLStreamException {

        var device = storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("id", deviceId)));
        var positions = PositionUtil.getPositions(storage, deviceId, from, to);

        var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        XMLStreamWriter writer = XMLOutputFactory.newFactory()
                .createXMLStreamWriter(outputStream, StandardCharsets.UTF_8.name());

        writer.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
        writer.writeStartElement("kml");
        writer.writeDefaultNamespace("http://www.opengis.net/kml/2.2");
        writer.writeStartElement("Document");
        writer.writeStartElement("name");
        writer.writeCharacters(device.getName());
        writer.writeEndElement();
        writer.writeStartElement("Placemark");
        writer.writeStartElement("name");
        writer.writeCharacters(dateFormat.format(from) + " - " + dateFormat.format(to));
        writer.writeEndElement();
        writer.writeStartElement("LineString");
        writer.writeStartElement("extrude");
        writer.writeCharacters("1");
        writer.writeEndElement();
        writer.writeStartElement("tessellate");
        writer.writeCharacters("1");
        writer.writeEndElement();
        writer.writeStartElement("altitudeMode");
        writer.writeCharacters("absolute");
        writer.writeEndElement();
        writer.writeStartElement("coordinates");
        writer.writeCharacters(positions.stream()
                .map(p -> String.format("%f,%f,%f", p.getLongitude(), p.getLatitude(), p.getAltitude()))
                .collect(Collectors.joining(" ")));
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

}
