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
        Double latitude, Double longitude) {

        ArrayList<MatrixReport> result = new ArrayList<>();
        List<List<Double>> sourceLocations = new ArrayList<>();
        ArrayList<Double> destinationLocation = new ArrayList<>();
        destinationLocation.add(longitude);
        destinationLocation.add(latitude);

        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            List<Double> deviceLocation = new ArrayList<>();
            deviceLocation.add(Context.getIdentityManager().getLastPosition(deviceId).getLongitude());
            deviceLocation.add(Context.getIdentityManager().getLastPosition(deviceId).getLatitude());
            sourceLocations.add(deviceLocation);
        }

        MatrixResponse matrixResponse = Context.getMatrix().getMatrix(sourceLocations, destinationLocation);

        int deviceIndex = 0;

        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            MatrixReport matrixReport = new MatrixReport();
            matrixReport.setDeviceId(deviceId);
            matrixReport.setDeviceName(Context.getIdentityManager().getById(deviceId).getName());
            matrixReport.setDistance(matrixResponse.getDistance(deviceIndex));
            matrixReport.setTime(matrixResponse.getDuration(deviceIndex) * 1000);
            result.add(matrixReport);
            deviceIndex++;
        }

        return result;
    }

    public static Collection<MatrixReport> getObjects(long userId, Collection<Long> deviceIds,
            Collection<Long> groupIds, Double latitude, Double longitude) {
        return calculateMatrixResult(userId, deviceIds, groupIds, latitude, longitude);
    }

}
