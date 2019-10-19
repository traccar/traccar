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
import org.traccar.directions.timeDistance.TimeDistanceResponse;
import org.traccar.reports.model.TimeDistanceReport;

public final class TimeDistance {

    private TimeDistance() {
    }

    private static ArrayList<TimeDistanceReport> calculateTimeDistanceResult(
        long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
        Double latitude, Double longitude) {

        ArrayList<TimeDistanceReport> result = new ArrayList<>();
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

        TimeDistanceResponse timeDistanceResponse = Context.getMatrix().getTimeDistanceMatrix(sourceLocations,
                destinationLocation);

        int deviceIndex = 0;

        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            TimeDistanceReport timeDistanceReport = new TimeDistanceReport();
            timeDistanceReport.setDeviceId(deviceId);
            timeDistanceReport.setDeviceName(Context.getIdentityManager().getById(deviceId).getName());
            timeDistanceReport.setDistance(timeDistanceResponse.getDistance(deviceIndex));
            timeDistanceReport.setTime(timeDistanceResponse.getDuration(deviceIndex) * 1000);
            result.add(timeDistanceReport);
            deviceIndex++;
        }

        return result;
    }

    public static Collection<TimeDistanceReport> getObjects(long userId, Collection<Long> deviceIds,
                                                            Collection<Long> groupIds,
                                                            Double latitude, Double longitude) {
        return calculateTimeDistanceResult(userId, deviceIds, groupIds, latitude, longitude);
    }

}
