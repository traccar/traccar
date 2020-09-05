/*
 * Copyright 2015 - 2020 Anton Tananaev (anton@traccar.org)
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

import org.traccar.Context;
import org.traccar.api.BaseResource;
import org.traccar.model.Position;
import org.traccar.web.CsvBuilder;
import org.traccar.web.GpxBuilder;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Path("positions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PositionResource extends BaseResource {

    public static final String TEXT_CSV = "text/csv";
    public static final String CONTENT_DISPOSITION_VALUE_CSV = "attachment; filename=positions.csv";
    public static final String GPX = "application/gpx+xml";
    public static final String CONTENT_DISPOSITION_VALUE_GPX = "attachment; filename=positions.gpx";

    @GET
    public Collection<Position> getJson(
            @QueryParam("deviceId") long deviceId, @QueryParam("id") List<Long> positionIds,
            @QueryParam("from") Date from, @QueryParam("to") Date to)
            throws SQLException {
        if (!positionIds.isEmpty()) {
            ArrayList<Position> positions = new ArrayList<>();
            for (Long positionId : positionIds) {
                Position position = Context.getDataManager().getObject(Position.class, positionId);
                Context.getPermissionsManager().checkDevice(getUserId(), position.getDeviceId());
                positions.add(position);
            }
            return positions;
        } else if (deviceId == 0) {
            return Context.getDeviceManager().getInitialState(getUserId());
        } else {
            Context.getPermissionsManager().checkDevice(getUserId(), deviceId);
            if (from != null && to != null) {
                return Context.getDataManager().getPositions(deviceId, from, to);
            } else {
                return Collections.singleton(Context.getDeviceManager().getLastPosition(deviceId));
            }
        }
    }

    @GET
    @Produces(TEXT_CSV)
    public Response getCsv(
            @QueryParam("deviceId") long deviceId, @QueryParam("from") Date from, @QueryParam("to") Date to)
            throws SQLException {
        Context.getPermissionsManager().checkDevice(getUserId(), deviceId);
        CsvBuilder csv = new CsvBuilder();
        csv.addHeaderLine(new Position());
        csv.addArray(Context.getDataManager().getPositions(deviceId, from, to));
        return Response.ok(csv.build()).header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE_CSV).build();
    }

    @GET
    @Produces(GPX)
    public Response getGpx(
            @QueryParam("deviceId") long deviceId, @QueryParam("from") Date from, @QueryParam("to") Date to)
            throws SQLException {
        Context.getPermissionsManager().checkDevice(getUserId(), deviceId);
        GpxBuilder gpx = new GpxBuilder(Context.getIdentityManager().getById(deviceId).getName());
        gpx.addPositions(Context.getDataManager().getPositions(deviceId, from, to));
        return Response.ok(gpx.build()).header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE_GPX).build();
    }

}
