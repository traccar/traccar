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
package org.traccar.reports;

import jakarta.inject.Inject;
import org.traccar.config.Config;
import org.traccar.model.Device;
import org.traccar.model.User;
import org.traccar.reports.common.ReportUtils;
import org.traccar.reports.model.GeofenceReportItem;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class GeofenceReportProvider {

    private final Config config;
    private final ReportUtils reportUtils;
    private final Storage storage;

    @Inject
    public GeofenceReportProvider(Config config, ReportUtils reportUtils, Storage storage) {
        this.config = config;
        this.reportUtils = reportUtils;
        this.storage = storage;
    }

    public Collection<GeofenceReportItem> getObjects(
            long userId, Collection<Long> geofenceIds,
            Date from, Date to) throws StorageException {
        reportUtils.checkPeriodLimit(from, to);

        ArrayList<GeofenceReportItem> result = new ArrayList<>();

        var devices = storage.getObjects(Device.class, new Request(
                new Columns.All(),
                new Condition.Permission(User.class, userId, Device.class)));

        for (long geofenceId: geofenceIds) {
            result.addAll(reportUtils.getGeofenceReport(geofenceId, devices, from, to));
        }

        return result;
    }

}
