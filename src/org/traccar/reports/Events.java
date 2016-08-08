package org.traccar.reports;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonArrayBuilder;

import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.web.CsvBuilder;
import org.traccar.web.JsonConverter;

public final class Events {

    private Events() {
    }

    public static String getJson(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Collection<String> types, Date from, Date to) throws SQLException {
        JsonArrayBuilder json = Json.createArrayBuilder();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            for (String type : types) {
                for (Event event : Context.getDataManager().getEvents(deviceId, type, from, to)) {
                    json.add(JsonConverter.objectToJson(event));
                }
            }
        }
        return json.build().toString();
    }

    public static String getCsv(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Collection<String> types, Date from, Date to) throws SQLException {
        CsvBuilder csv = new CsvBuilder();
        csv.addHeaderLine(new Event());
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            for (String type : types) {
                csv.addArray(Context.getDataManager().getEvents(deviceId, type, from, to));
            }
        }
        return csv.build();
    }
}
