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

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;

public final class ReportUtils {

    private ReportUtils() {
    }

    public static String getDistanceUnit(long userId) {
        String unit = Context.getPermissionsManager().getUser(userId).getDistanceUnit();
        if (unit == null) {
            unit  = Context.getPermissionsManager().getServer().getDistanceUnit();
        }
        return unit != null ? unit : "km";
    }

    public static String getSpeedUnit(long userId) {
        String unit = Context.getPermissionsManager().getUser(userId).getSpeedUnit();
        if (unit == null) {
            unit  = Context.getPermissionsManager().getServer().getSpeedUnit();
        }
        return unit != null ? unit : "kn";
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
        firstOdometer = firstPosition.getDouble(Position.KEY_ODOMETER);
        lastOdometer = lastPosition.getDouble(Position.KEY_ODOMETER);

        if (useOdometer && (firstOdometer != 0.0 || lastOdometer != 0.0)) {
            distance = lastOdometer - firstOdometer;
        } else if (firstPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)
                && lastPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)) {
            distance = lastPosition.getDouble(Position.KEY_TOTAL_DISTANCE)
                    - firstPosition.getDouble(Position.KEY_TOTAL_DISTANCE);
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
                        BigDecimal v = new BigDecimal(firstPosition.getAttributes().get(Position.KEY_FUEL).toString());
                        v = v.subtract(new BigDecimal(lastPosition.getAttributes().get(Position.KEY_FUEL).toString()));
                        return v.setScale(2, RoundingMode.HALF_EVEN).toString() + " %";
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
