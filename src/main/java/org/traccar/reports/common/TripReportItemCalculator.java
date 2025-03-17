/*
 * Copyright 2016 - 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.reports.common;

import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.reports.model.TripReportItem;
import org.traccar.storage.StorageException;

/**
 * Calculator for trip report items.
 */
public class TripReportItemCalculator implements TripStopReportCalculator<TripReportItem> {

    private final ReportUtils reportUtils;

    public TripReportItemCalculator(ReportUtils reportUtils) {
        this.reportUtils = reportUtils;
    }

    @Override
    public TripReportItem calculate(
            Device device, Position startPosition, Position endPosition,
            double maxSpeed, boolean ignoreOdometer) throws StorageException {
        return reportUtils.calculateTrip(device, startPosition, endPosition, maxSpeed, ignoreOdometer);
    }
}
