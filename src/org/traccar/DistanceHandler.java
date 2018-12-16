/*
 * Copyright 2015 Amila Silva
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
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
package org.traccar;

import org.traccar.helper.DistanceCalculator;
import org.traccar.model.Position;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DistanceHandler extends BaseDataHandler {

    private static final long MILLIS_IN_SECOND = 1000L;
    private static final long MAX_DATA_LOSS_DURATION_SECONDS = 1800L;

    private final boolean filter;
    private final int coordinatesMinError;
    private final int coordinatesMaxError;

    public DistanceHandler(boolean filter, int coordinatesMinError, int coordinatesMaxError) {
        this.filter = filter;
        this.coordinatesMinError = coordinatesMinError;
        this.coordinatesMaxError = coordinatesMaxError;
    }

    private Position getLastPosition(long deviceId) {
        if (Context.getIdentityManager() != null) {
            return Context.getIdentityManager().getLastPosition(deviceId);
        }
        return null;
    }

    @Override
    protected Position handlePosition(Position position) {

        double distance = 0.0;
        if (position.getAttributes().containsKey(Position.KEY_DISTANCE)) {
            distance = position.getDouble(Position.KEY_DISTANCE);
        }
        double totalDistance = 0.0;

        Position last = getLastPosition(position.getDeviceId());
        if (last != null && position.getDeviceTime().compareTo((last.getDeviceTime())) >= 0) {
            totalDistance = last.getDouble(Position.KEY_TOTAL_DISTANCE);

            // If the current position is not valid (esp when lat=long=0), we want to carry forward the
            // total distance on it, so that calculations on future positions stay correct.
            if (!position.getValid()) {
                position.set(Position.KEY_DISTANCE, 0);
                position.set(Position.KEY_TOTAL_DISTANCE, totalDistance);
                return position;
            }

            if (!position.getAttributes().containsKey(Position.KEY_DISTANCE)) {
                distance = DistanceCalculator.distance(
                        position.getLatitude(), position.getLongitude(),
                        last.getLatitude(), last.getLongitude());
                distance = BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
            }

            // If data missing then check odometer readings. Knowing ignition was on may not help in this case, coz we
            // don't know for sure what happened during data loss.
            long durationBetweenPacketsSeconds =
                    (position.getDeviceTime().getTime() - last.getDeviceTime().getTime()) / MILLIS_IN_SECOND;

            if (durationBetweenPacketsSeconds >= MAX_DATA_LOSS_DURATION_SECONDS
                && last.getAttributes().containsKey(Position.KEY_ODOMETER)
                && position.getAttributes().containsKey(Position.KEY_ODOMETER)) {

                double differenceInOdometer = (double) (Integer) position.getAttributes().get(Position.KEY_ODOMETER)
                                              - (double) (Integer)last.getAttributes().get(Position.KEY_ODOMETER);
                if(differenceInOdometer > distance) {
                    distance = differenceInOdometer;
                }
            }

            if (filter && last.getValid() && last.getLatitude() != 0 && last.getLongitude() != 0) {
                boolean satisfiesMin = coordinatesMinError == 0 || distance > coordinatesMinError;
                boolean satisfiesMax = coordinatesMaxError == 0
                        || distance < coordinatesMaxError || position.getValid();
                if (!satisfiesMin || !satisfiesMax) {
                    position.setLatitude(last.getLatitude());
                    position.setLongitude(last.getLongitude());
                    distance = 0;
                }
            }
        }
        position.set(Position.KEY_DISTANCE, distance);
        totalDistance = BigDecimal.valueOf(totalDistance + distance).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
        position.set(Position.KEY_TOTAL_DISTANCE, totalDistance);

        return position;
    }

}
