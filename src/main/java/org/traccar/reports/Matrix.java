/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 Andrey Kunitsyn (andrey@traccar.org)
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.traccar.Context;
import org.traccar.model.Position;
import org.traccar.reports.model.MatrixReport;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.json.JsonObject;


public final class Matrix {

    private Matrix() {
    }

    private static MatrixReport calculateMatrixResult(
        long deviceId, Double destLat, Double destLon)throws SQLException {
        MatrixReport result = new MatrixReport();
        Position last = Context.getIdentityManager().getLastPosition(deviceId);
        double devLat = last.getLatitude();
        double devLon = last.getLongitude();

        result.setDeviceId(deviceId);
        result.setDeviceName(Context.getIdentityManager().getById(deviceId).getName());
        result.setDestLat(destLat);
        result.setDestLon(destLon);

        System.out.println(devLat);
        System.out.println(devLon);
        System.out.println(destLat);
        System.out.println(destLon);

//        Context.getDirections().getMatrix(45.44, 343.334, 34.234, 3434.3);

        double distance = 0;
        int duration = 0;
        int sources = 0;
        int destinations = 1;
        String service = Context.getConfig().getString("matrix.server");
        System.out.println(service);
        String key = Context.getConfig().getString("matrix.key");
        System.out.println(key);

        if (service.equals("locationiq")) {
            String baseUrl = "https://us1.locationiq.com/v1/matrix/driving/";
            String annotations = "distance,duration";
            String finalUrl = String.format(
                "%s%f,%f;%f,%f?sources=%d&destinations=%d&annotations=%s&key=%s",
                baseUrl, devLon, devLat, destLon, destLat, sources, destinations, annotations, key);

            Invocation.Builder request = Context.getClient().target(finalUrl)
                .request();

            JsonObject result1 = request.get(JsonObject.class);
            System.out.println(result1);

            distance = result1.getJsonArray("distances").getJsonArray(0).getJsonNumber(0).doubleValue();
            System.out.println(distance);

            duration = result1.getJsonArray("durations").getJsonArray(0).getJsonNumber(0).intValue();
            System.out.println(duration);
        } else if (service.equals("openroute")) {
            String baseUrl = "https://api.openrouteservice.org/v2/matrix/driving-car";

            String payload1 = String.format("{\"locations\":[[%f,%f],[%f,%f]],",
                devLon, devLat, destLon, destLat);

            String payload2 = String.format("\"sources\":[%d],\"destinations\":[%d],",
                sources, destinations);

            String payload3 = String.format("\"metrics\":[\"distance\",\"duration\"]}");

            Entity<String> payload = Entity.json(payload1 + payload2 + payload3);

            Response request = Context.getClient().target(baseUrl)
                .request()
                .header("Authorization", key)
                .header("Accept",
                    "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8")
                .post(payload);

            JsonObject result1 = request.readEntity(JsonObject.class);
            System.out.println(result1);

            distance = result1.getJsonArray("distances").getJsonArray(0).getJsonNumber(0).doubleValue();
            System.out.println(distance);

            duration = result1.getJsonArray("durations").getJsonArray(0).getJsonNumber(0).intValue();
            System.out.println(duration);
        }

        result.setTime(duration * 1000);
        result.setDistance(distance);

        return result;
    }

    public static Collection<MatrixReport> getObjects(long userId, Collection<Long> deviceIds,
            Collection<Long> groupIds, Double destLat, Double destLon) throws SQLException {
        ArrayList<MatrixReport> result = new ArrayList<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            result.add(calculateMatrixResult(deviceId, destLat, destLon));
        }
        return result;
    }

}
