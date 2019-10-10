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
import java.util.List;
import java.util.Collection;

import org.traccar.Context;
import org.traccar.reports.model.MatrixReport;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public final class Matrix {

    private Matrix() {
    }

    private static ArrayList<MatrixReport> calculateMatrixResult(
        long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
        Double destLat, Double destLon)
        throws SQLException {

        ArrayList<MatrixReport> result = new ArrayList<>();
        List<List<Double>> sourceCoord = new ArrayList<List<Double>>();
        ArrayList<Double> destCoord = new ArrayList<>();
        destCoord.add(destLon);
        destCoord.add(destLat);

//        Context.getDirections().getMatrix(45.44, 343.334, 34.234, 3434.3);

        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            List<Double> devCoord = new ArrayList<>();
            devCoord.add(Context.getIdentityManager().getLastPosition(deviceId).getLongitude());
            devCoord.add(Context.getIdentityManager().getLastPosition(deviceId).getLatitude());
            sourceCoord.add(devCoord);
        }

        String service = Context.getConfig().getString("matrix.server");
        String key = Context.getConfig().getString("matrix.key");

        JsonArray distances = Json.createArrayBuilder().build();
        JsonArray durations = Json.createArrayBuilder().build();

        if (service.equals("locationiq")) {
            String baseUrl = "https://us1.locationiq.com/v1/matrix/driving/";
            int destinationsLocationIq = 0;
            String annotations = "distance,duration";

            String coordLocationIq = "";
            String sourcesLocationIq = "";

            int i = 1;
            for (List<Double> coord : sourceCoord) {
                int j = 1;
                for (double point : coord) {
                    coordLocationIq += point;
                    if (j < coord.size()) {
                        coordLocationIq += ",";
                    }
                    j++;
                }
                sourcesLocationIq += i;
                if (i < sourceCoord.size()) {
                    coordLocationIq += ";";
                    sourcesLocationIq += ";";
                }
                i++;
            }

            String destLocationIq = "";
            int j = 1;
            for (double point : destCoord) {
                destLocationIq += point;
                if (j < destCoord.size()) {
                    destLocationIq += ",";
                }
                j++;
            }

            String finalUrl = String.format(
                "%s%s;%s?destinations=%d&sources=%s&annotations=%s&key=%s",
                baseUrl, destLocationIq, coordLocationIq, destinationsLocationIq, sourcesLocationIq, annotations, key);

            Invocation.Builder request = Context.getClient().target(finalUrl)
                .request();

            JsonObject result1 = request.get(JsonObject.class);

            distances = result1.getJsonArray("distances");

            durations = result1.getJsonArray("durations");

        } else if (service.equals("openroute")) {
            String baseUrl = "https://api.openrouteservice.org/v2/matrix/driving-car";

            List<List<Double>> coordOpenRoute = sourceCoord;
            coordOpenRoute.add(destCoord);
            int destinationsOpenRoute = coordOpenRoute.size() - 1;

            List<Integer> sourcesOpenRoute = new ArrayList<>();
            int i = 0;
            for (i = 0; i < (coordOpenRoute.size() - 1); i++) {
                sourcesOpenRoute.add(i);
            }

            String payload1 = String.format("{\"locations\":%s,", coordOpenRoute);

            String payload2 = String.format("\"sources\":%s,\"destinations\":[%d],",
                sourcesOpenRoute, destinationsOpenRoute);

            String payload3 = String.format("\"metrics\":[\"distance\",\"duration\"]}");

            Entity<String> payload = Entity.json(payload1 + payload2 + payload3);

            Response request = Context.getClient().target(baseUrl)
                .request()
                .header("Authorization", key)
                .header("Accept",
                    "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8")
                .post(payload);

            JsonObject result1 = request.readEntity(JsonObject.class);

            distances = result1.getJsonArray("distances");

            durations = result1.getJsonArray("durations");
        }

        int dev = 0;

        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            MatrixReport matrixReport = new MatrixReport();
            matrixReport.setDeviceId(deviceId);
            matrixReport.setDeviceName(Context.getIdentityManager().getById(deviceId).getName());
            matrixReport.setDestLat(destLat);
            matrixReport.setDestLon(destLon);
            matrixReport.setDevLat(Context.getIdentityManager().getLastPosition(deviceId).getLatitude());
            matrixReport.setDevLon(Context.getIdentityManager().getLastPosition(deviceId).getLongitude());
            matrixReport.setDistance(distances.getJsonArray(dev).getJsonNumber(0).doubleValue());
            matrixReport.setTime(durations.getJsonArray(dev).getJsonNumber(0).intValue() * 1000);
            result.add(matrixReport);
            dev++;
        }

        return result;
    }

    public static Collection<MatrixReport> getObjects(long userId, Collection<Long> deviceIds,
            Collection<Long> groupIds, Double destLat, Double destLon) throws SQLException {
        ArrayList<MatrixReport> result = new ArrayList<>();
        result = calculateMatrixResult(userId, deviceIds, groupIds, destLat, destLon);
        return result;
    }

}
