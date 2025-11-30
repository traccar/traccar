/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.api.resource;

import org.traccar.api.BaseResource;
import org.traccar.api.dto.ChildStatusDto;
import org.traccar.api.dto.WeeklyReportDto;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.ChildHealthManager;
import org.traccar.database.ChildProfileManager;
import org.traccar.model.ChildHealthRecord;
import org.traccar.model.ChildProfile;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Path("savekid/children")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SavekidChildResource extends BaseResource {

    private static final int HEART_RATE_LOW = 50;
    private static final int HEART_RATE_HIGH = 120;
    private static final double BODY_TEMP_LOW = 35.0;
    private static final double BODY_TEMP_HIGH = 38.0;

    @Inject
    private Config config;

    @Inject
    private ChildProfileManager childProfileManager;

    @Inject
    private ChildHealthManager childHealthManager;

    @POST
    public Response add(ChildProfile profile) throws Exception {
        permissionsService.checkEdit(getUserId(), ChildProfile.class, true, false);
        permissionsService.checkPermission(Device.class, getUserId(), profile.getDeviceId());

        Date now = new Date();
        if (profile.getCreatedAt() == null) {
            profile.setCreatedAt(now);
        }
        profile.setUpdatedAt(now);

        ChildProfile stored = childProfileManager.addProfile(profile);
        return Response.status(Response.Status.CREATED).entity(stored).build();
    }

    @Path("{id}")
    @GET
    public ChildProfile get(@PathParam("id") long id) throws StorageException {
        return getAuthorizedChild(id);
    }

    @Path("by-device/{deviceId}")
    @GET
    public Response getByDevice(@PathParam("deviceId") long deviceId) throws StorageException {
        permissionsService.checkPermission(Device.class, getUserId(), deviceId);
        List<ChildProfile> profiles = childProfileManager.getProfilesByDevice(deviceId);
        if (profiles.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(profiles.get(0)).build();
    }

    @Path("{id}/status")
    @GET
    public ChildStatusDto getStatus(@PathParam("id") long id) throws StorageException {
        ChildProfile child = getAuthorizedChild(id);

        Position lastPosition = storage.getObject(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions(child.getDeviceId())));

        ChildHealthRecord lastHealth = latestHealthRecord(child.getId());
        Event lastEvent = latestEvent(child.getDeviceId());

        ChildStatusDto dto = new ChildStatusDto();
        dto.setChildId(child.getId());
        dto.setLastPositionTime(lastPosition != null ? lastPosition.getFixTime() : null);
        if (lastHealth != null) {
            dto.setLastHeartRate(lastHealth.getHeartRate());
            dto.setLastBodyTemp(lastHealth.getBodyTemp());
        }

        dto.setLastEventType(resolveEventType(lastEvent, lastHealth));
        dto.setStatus(resolveStatus(lastEvent, lastHealth, lastPosition));
        return dto;
    }

    @Path("{id}/health-history")
    @GET
    public List<ChildHealthRecord> getHealthHistory(
            @PathParam("id") long id, @QueryParam("from") Date from, @QueryParam("to") Date to)
            throws StorageException {
        ChildProfile child = getAuthorizedChild(id);
        return childHealthManager.getHealthHistory(child.getId(), from, to);
    }

    @Path("{id}/weekly-report")
    @GET
    public WeeklyReportDto getWeeklyReport(@PathParam("id") long id) throws StorageException {
        ChildProfile child = getAuthorizedChild(id);
        Date to = new Date();
        Date from = new Date(System.currentTimeMillis() - Duration.ofDays(7).toMillis());

        List<ChildHealthRecord> records = childHealthManager.getHealthHistory(child.getId(), from, to);

        WeeklyReportDto report = new WeeklyReportDto();
        report.setChildId(child.getId());

        int heartRateCount = 0;
        int heartRateTotal = 0;
        int temperatureCount = 0;
        double temperatureTotal = 0;
        long totalSteps = 0;
        int alerts = 0;
        long sleepMillis = 0;

        for (int i = 0; i < records.size(); i++) {
            ChildHealthRecord record = records.get(i);
            if (record.getHeartRate() != null) {
                heartRateTotal += record.getHeartRate();
                heartRateCount += 1;
            }
            if (record.getBodyTemp() != null) {
                temperatureTotal += record.getBodyTemp();
                temperatureCount += 1;
            }
            if (record.getSteps() != null) {
                totalSteps += record.getSteps();
            }
            if (healthAnomaly(record) != null) {
                alerts += 1;
            }
            if (isSleeping(record)) {
                Date periodStart = record.getServerTime();
                Date periodEnd = i < records.size() - 1 ? records.get(i + 1).getServerTime() : to;
                if (periodStart != null && periodEnd != null) {
                    sleepMillis += Math.max(0, periodEnd.getTime() - periodStart.getTime());
                }
            }
        }

        if (heartRateCount > 0) {
            report.setAverageHeartRate((double) heartRateTotal / heartRateCount);
        }
        if (temperatureCount > 0) {
            report.setAverageBodyTemp(temperatureTotal / temperatureCount);
        }
        report.setTotalSteps(totalSteps);
        if (sleepMillis > 0) {
            report.setSleepHours(sleepMillis / 1000d / 60 / 60);
        }
        report.setAlertCount(alerts);
        return report;
    }

    private ChildProfile getAuthorizedChild(long id) throws StorageException {
        ChildProfile child = childProfileManager.getProfile(id);
        if (child == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }
        permissionsService.checkPermission(Device.class, getUserId(), child.getDeviceId());
        return child;
    }

    private ChildHealthRecord latestHealthRecord(long childId) throws StorageException {
        List<ChildHealthRecord> records = storage.getObjects(ChildHealthRecord.class, new Request(
                new Columns.All(), new Condition.Equals("childId", childId), new Order("servertime", true, 1)));
        return records.isEmpty() ? null : records.get(0);
    }

    private Event latestEvent(long deviceId) throws StorageException {
        List<Event> events = storage.getObjects(Event.class, new Request(
                new Columns.All(), new Condition.Equals("deviceId", deviceId), new Order("eventTime", true, 1)));
        return events.isEmpty() ? null : events.get(0);
    }

    private String resolveStatus(Event lastEvent, ChildHealthRecord lastHealth, Position position) {
        if (isSosEvent(lastEvent)) {
            return "SOS";
        }
        if (healthAnomaly(lastHealth) != null) {
            return "RISK";
        }
        if (position == null || isPositionStale(position)) {
            return "ALERT";
        }
        return "OK";
    }

    private String resolveEventType(Event lastEvent, ChildHealthRecord lastHealth) {
        if (isSosEvent(lastEvent)) {
            return "SOS";
        }
        String healthEvent = healthAnomaly(lastHealth);
        if (healthEvent != null) {
            return healthEvent;
        }
        return Optional.ofNullable(lastEvent).map(Event::getType).orElse(null);
    }

    private boolean isSosEvent(Event event) {
        if (event == null) {
            return false;
        }
        if (Event.TYPE_ALARM.equals(event.getType())) {
            Object alarm = event.getAttributes().get(Position.KEY_ALARM);
            return Position.ALARM_SOS.equals(alarm);
        }
        return false;
    }

    private String healthAnomaly(ChildHealthRecord health) {
        if (health == null) {
            return null;
        }
        if (health.getHeartRate() != null) {
            if (health.getHeartRate() > HEART_RATE_HIGH) {
                return "HR_HIGH";
            }
            if (health.getHeartRate() < HEART_RATE_LOW) {
                return "HR_LOW";
            }
        }
        if (health.getBodyTemp() != null) {
            if (health.getBodyTemp() > BODY_TEMP_HIGH) {
                return "TEMP_HIGH";
            }
            if (health.getBodyTemp() < BODY_TEMP_LOW) {
                return "TEMP_LOW";
            }
        }
        return null;
    }

    private boolean isPositionStale(Position position) {
        if (position == null) {
            return true;
        }
        Date referenceTime = Optional.ofNullable(position.getFixTime()).orElse(position.getDeviceTime());
        if (referenceTime == null) {
            return true;
        }
        long timeoutSeconds = config.getLong(Keys.STATUS_TIMEOUT);
        long threshold = System.currentTimeMillis() - Duration.ofSeconds(timeoutSeconds).toMillis();
        return referenceTime.getTime() < threshold;
    }

    private boolean isSleeping(ChildHealthRecord record) {
        if (record.getSleepStatus() == null) {
            return false;
        }
        return record.getSleepStatus().toLowerCase(Locale.ROOT).contains("sleep");
    }
}
