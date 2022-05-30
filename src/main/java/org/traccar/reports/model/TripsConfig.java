/*
 * Copyright 2017 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.reports.model;

public class TripsConfig {

    public TripsConfig(double minimalTripDistance, long minimalTripDuration, long minimalParkingDuration,
            long minimalNoDataDuration, boolean useIgnition, boolean processInvalidPositions, double speedThreshold) {
        this.minimalTripDistance = minimalTripDistance;
        this.minimalTripDuration = minimalTripDuration;
        this.minimalParkingDuration = minimalParkingDuration;
        this.minimalNoDataDuration = minimalNoDataDuration;
        this.useIgnition = useIgnition;
        this.processInvalidPositions = processInvalidPositions;
        this.speedThreshold = speedThreshold;
    }

    private final double minimalTripDistance;

    public double getMinimalTripDistance() {
        return minimalTripDistance;
    }

    private final long minimalTripDuration;

    public long getMinimalTripDuration() {
        return minimalTripDuration;
    }

    private final long minimalParkingDuration;

    public long getMinimalParkingDuration() {
        return minimalParkingDuration;
    }

    private final long minimalNoDataDuration;

    public long getMinimalNoDataDuration() {
        return minimalNoDataDuration;
    }

    private final boolean useIgnition;

    public boolean getUseIgnition() {
        return useIgnition;
    }

    private final boolean processInvalidPositions;

    public boolean getProcessInvalidPositions() {
        return processInvalidPositions;
    }

    private final double speedThreshold;

    public double getSpeedThreshold() {
        return speedThreshold;
    }

}
