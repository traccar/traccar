package org.traccar.reports;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.web.CsvBuilder;
import org.traccar.web.JsonConverter;

public final class Events {

    private Events() {
    }

    public static String getJson(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Collection<String> types, Date from, Date to) throws SQLException {
        JsonObjectBuilder json = Json.createObjectBuilder();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            for (String type : types) {
                json.add(String.valueOf(deviceId), JsonConverter.arrayToJson(Context.getDataManager()
                        .getEvents(deviceId, type, from, to)));
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
