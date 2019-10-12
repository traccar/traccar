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
import org.traccar.directions.matrix.MatrixResponse;
import org.traccar.reports.model.MatrixReport;

public final class Matrix {

    private Matrix() {
    }

    private static ArrayList<MatrixReport> calculateMatrixResult(
        long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
        Double destLat, Double destLon)
        throws SQLException {

        ArrayList<MatrixReport> result = new ArrayList<>();
        List<List<Double>> sourceCoord = new ArrayList<>();
        ArrayList<Double> destCoord = new ArrayList<>();
        destCoord.add(destLon);
        destCoord.add(destLat);


        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            List<Double> devCoord = new ArrayList<>();
            devCoord.add(Context.getIdentityManager().getLastPosition(deviceId).getLongitude());
            devCoord.add(Context.getIdentityManager().getLastPosition(deviceId).getLatitude());
            sourceCoord.add(devCoord);
        }

        MatrixResponse matrixResponse = Context.getMatrix().getMatrix(sourceCoord, destCoord);

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
            matrixReport.setDistance(matrixResponse.getDistances().getJsonArray(dev).getJsonNumber(0).doubleValue());
            matrixReport.setTime(matrixResponse.getDurations().getJsonArray(dev).getJsonNumber(0).intValue() * 1000);
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
