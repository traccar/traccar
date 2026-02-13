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

import org.traccar.helper.DateUtil;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;

public class GpxExportProvider {

    private final Storage storage;

    @Inject
    public GpxExportProvider(Storage storage) {
        this.storage = storage;
    }

    public void generate(
            OutputStream outputStream, long deviceId, Date from, Date to) throws StorageException {

        var device = storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("id", deviceId)));
        var positions = PositionUtil.getPositions(storage, deviceId, from, to);

        try (PrintWriter writer = new PrintWriter(outputStream)) {
            writer.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.print("<gpx version=\"1.0\">");
            writer.print("<trk>");
            writer.print("<name>");
            writer.print(device.getName());
            writer.print("</name>");
            writer.print("<trkseg>");
            positions.forEach(position -> {
                writer.print("<trkpt lat=\"");
                writer.print(position.getLatitude());
                writer.print("\" lon=\"");
                writer.print(position.getLongitude());
                writer.print("\">");
                writer.print("<ele>");
                writer.print(position.getAltitude());
                writer.print("</ele>");
                writer.print("<time>");
                writer.print(DateUtil.formatDate(position.getFixTime()));
                writer.print("</time>");
                writer.print("</trkpt>");
            });
            writer.print("</trkseg>");
            writer.print("</trk>");
            writer.print("</gpx>");
        }
    }

}
