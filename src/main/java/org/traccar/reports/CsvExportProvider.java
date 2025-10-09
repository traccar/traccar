/*
 * Copyright 2022 - 2025 Anton Tananaev (anton@traccar.org)
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

import org.traccar.api.security.PermissionsService;
import org.traccar.helper.model.PositionUtil;
import org.traccar.helper.model.UserUtil;
import org.traccar.model.Geofence;
import org.traccar.model.Position;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;

import jakarta.inject.Inject;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CsvExportProvider {

    private final Storage storage;
    private final PermissionsService permissionsService;

    @Inject
    public CsvExportProvider(Storage storage, PermissionsService permissionsService) {
        this.storage = storage;
        this.permissionsService = permissionsService;
    }

    public void generate(
            OutputStream outputStream, long userId, long deviceId, long geofenceId,
            Date from, Date to) throws StorageException {

        var server = permissionsService.getServer();
        var user = permissionsService.getUser(userId);
        var positions = PositionUtil.getPositions(storage, deviceId, from, to);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(UserUtil.getTimezone(server, user));

        Geofence geofence = geofenceId == 0 ? null : storage.getObject(Geofence.class, new Request(
                new Columns.All(), new Condition.Equals("id", geofenceId)));

        var attributes = positions.stream()
                .filter(position -> geofence == null || geofence.containsPosition(position))
                .flatMap(position -> position.getAttributes().keySet().stream())
                .collect(Collectors.toUnmodifiableSet());

        var properties = new LinkedHashMap<String, Function<Position, Object>>();
        properties.put("id", Position::getId);
        properties.put("deviceId", Position::getDeviceId);
        properties.put("protocol", Position::getProtocol);
        properties.put("serverTime", position -> dateFormat.format(position.getServerTime()));
        properties.put("deviceTime", position -> dateFormat.format(position.getDeviceTime()));
        properties.put("fixTime", position -> dateFormat.format(position.getFixTime()));
        properties.put("valid", Position::getValid);
        properties.put("latitude", Position::getLatitude);
        properties.put("longitude", Position::getLongitude);
        properties.put("altitude", Position::getAltitude);
        properties.put("speed", Position::getSpeed);
        properties.put("course", Position::getCourse);
        properties.put("address", Position::getAddress);
        properties.put("accuracy", Position::getAccuracy);
        attributes.forEach(key -> properties.put(key, position -> position.getAttributes().get(key)));

        try (PrintWriter writer = new PrintWriter(outputStream)) {
            writer.println(String.join(",", properties.keySet()));
            positions.forEach(position -> writer.println(properties.values().stream()
                    .map(f -> Objects.toString(f.apply(position), ""))
                    .collect(Collectors.joining(","))));
        }
    }

}
