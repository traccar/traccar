/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
 * Copyright 2016 Andrey Kunitsyn (abyss@fox5.ru)
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public final class ReportUtils {

    private ReportUtils() {
    }

    public static Collection<Long> getDeviceList(Collection<Long> deviceIds, Collection<Long> groupIds) {
        Collection<Long> result = new ArrayList<>();
        result.addAll(deviceIds);
        for (long groupId : groupIds) {
            result.addAll(Context.getPermissionsManager().getGroupDevices(groupId));
        }
        return result;
    }

    public static double calculateDistance(Position firstPosition, Position lastPosition) {
        return calculateDistance(firstPosition, lastPosition, true);
    }

    public static double calculateDistance(Position firstPosition, Position lastPosition, boolean useOdometer) {
        double distance = 0.0;
        double firstOdometer = 0.0;
        double lastOdometer = 0.0;
        if (firstPosition.getAttributes().containsKey(Position.KEY_ODOMETER)) {
            firstOdometer = ((Number) firstPosition.getAttributes().get(Position.KEY_ODOMETER)).doubleValue();
        }
        if (lastPosition.getAttributes().containsKey(Position.KEY_ODOMETER)) {
            lastOdometer = ((Number) lastPosition.getAttributes().get(Position.KEY_ODOMETER)).doubleValue();
        }
        if (useOdometer && (firstOdometer != 0.0 || lastOdometer != 0.0)) {
            distance = lastOdometer - firstOdometer;
        } else if (firstPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)
                && lastPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)) {
            distance = ((Number) lastPosition.getAttributes().get(Position.KEY_TOTAL_DISTANCE)).doubleValue()
                    - ((Number) firstPosition.getAttributes().get(Position.KEY_TOTAL_DISTANCE)).doubleValue();
        }
        return distance;
    }

    public static String calculateFuel(Position firstPosition, Position lastPosition) {

        if (firstPosition.getAttributes().get(Position.KEY_FUEL) != null
                && lastPosition.getAttributes().get(Position.KEY_FUEL) != null) {
            try {
                switch (firstPosition.getProtocol()) {
                case "meitrack":
                case "galileo":
                case "noran":
                    return new BigDecimal(firstPosition.getAttributes().get(Position.KEY_FUEL).toString())
                            .subtract(new BigDecimal(lastPosition.getAttributes().get(Position.KEY_FUEL).toString()))
                            .setScale(2, RoundingMode.HALF_EVEN).toString() + " %";
                default:
                    break;
                }
            } catch (Exception error) {
                Log.warning(error);
            }
        }
        return "-";
    }

}
