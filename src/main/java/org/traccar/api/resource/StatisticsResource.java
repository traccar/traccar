/*
 * Copyright 2016 - 2026 Anton Tananaev (anton@traccar.org)
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
package org.traccar.api.resource;

import org.traccar.api.BaseResource;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.model.Statistics;
import org.traccar.model.User;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Path("statistics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StatisticsResource extends BaseResource {

    @GET
    public Stream<Statistics> get(
            @QueryParam("from") Date from, @QueryParam("to") Date to) throws StorageException {
        permissionsService.checkAdmin(getUserId());
        return storage.getObjectsStream(Statistics.class, new Request(
                new Columns.All(),
                new Condition.Between("captureTime", from, to),
                new Order("captureTime")));
    }

    public record DeviceStatistics(long deviceId, String deviceName, long messagesStored) {
    }

    @GET
    @Path("devices")
    public List<DeviceStatistics> getDevices(
            @QueryParam("from") Date from, @QueryParam("to") Date to,
            @QueryParam("userId") long userId) throws StorageException {
        permissionsService.checkAdmin(getUserId());
        if (to.getTime() - from.getTime() > TimeUnit.DAYS.toMillis(1)) {
            throw new IllegalArgumentException("Time period must not exceed 24 hours");
        }
        long selectedUserId = userId > 0 ? userId : getUserId();
        List<Device> devices = storage.getObjects(Device.class, new Request(
                new Columns.Include("id", "name"),
                new Condition.Permission(User.class, selectedUserId, Device.class)));
        List<DeviceStatistics> result = new ArrayList<>();
        for (Device device : devices) {
            try (var positions = storage.getObjectsStream(Position.class, new Request(
                    new Columns.Include("id"),
                    new Condition.And(
                            new Condition.Equals("deviceId", device.getId()),
                            new Condition.Between("fixTime", from, to))))) {
                result.add(new DeviceStatistics(device.getId(), device.getName(), positions.count()));
            }
        }
        return result;
    }

}
