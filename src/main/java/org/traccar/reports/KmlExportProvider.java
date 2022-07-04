/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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

import javax.inject.Inject;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;

public class KmlExportProvider {

    private final Storage storage;

    @Inject
    public KmlExportProvider(Storage storage) {
        this.storage = storage;
    }

    public void generate(
            OutputStream outputStream, long deviceId, Date from, Date to) throws StorageException {

        var device = storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("id", "id", deviceId)));
        var positions = PositionUtil.getPositions(storage, deviceId, from, to);

        var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        try (PrintWriter writer = new PrintWriter(outputStream)) {
            writer.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.print("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");
            writer.print("<Document>");
            writer.print("<name>");
            writer.print(device.getName());
            writer.print("</name>");
            writer.print("<Placemark>");
            writer.print("<name>");
            writer.print(dateFormat.format(from));
            writer.print(" - ");
            writer.print(dateFormat.format(to));
            writer.print("</name>");
            writer.print("<LineString>");
            writer.print("<extrude>1</extrude>");
            writer.print("<tessellate>1</tessellate>");
            writer.print("<altitudeMode>absolute</altitudeMode>");
            writer.print("<coordinates>");
            writer.print(positions.stream()
                    .map((p -> String.format("%f,%f,%f", p.getLongitude(), p.getLatitude(), p.getAltitude())))
                    .collect(Collectors.joining(" ")));
            writer.print("</coordinates>");
            writer.print("</LineString>");
            writer.print("</Placemark>");
            writer.print("</Document>");
            writer.print("</kml>");
        }
    }

}
