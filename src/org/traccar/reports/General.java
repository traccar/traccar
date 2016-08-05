package org.traccar.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.traccar.Context;
import org.traccar.helper.DistanceCalculator;
import org.traccar.model.Position;
import org.traccar.web.CsvBuilder;
import org.traccar.web.JsonConverter;

public final class General {

    private General() {
    }

    public static class GeneralResult {

        private String deviceName;
        public String getDeviceName() {
            return deviceName;
        }
        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }

        private double distance = 0;
        public double getDistance() {
            return distance;
        }
        public void setDistance(double distance) {
            this.distance = distance;
        }

        public void addDistance(double distance) {
            this.distance += distance;
        }

        private double averageSpeed = 0;
        public double getAverageSpeed() {
            return averageSpeed;
        }
        public void setAverageSpeed(double averageSpeed) {
            this.averageSpeed = averageSpeed;
        }

        private double maxSpeed = 0;
        public double getMaxSpeed() {
            return maxSpeed;
        }
        public void setMaxSpeed(double maxSpeed) {
            if (maxSpeed > this.maxSpeed) {
                this.maxSpeed = maxSpeed;
            }
        }
    }

    private static GeneralResult calculateGeneralResult(long deviceId, Date from, Date to) throws SQLException {
        GeneralResult result = new GeneralResult();
        Collection<Position> positions = Context.getDataManager().getPositions(deviceId, from, to);
        if (positions != null && !positions.isEmpty()) {
            result.setDeviceName(Context.getDeviceManager().getDeviceById(deviceId).getName());
            Position previousPosition = null;
            double speedSum = 0;
            for (Position position : positions) {
                if (previousPosition != null) {
                    result.addDistance(DistanceCalculator.distance(previousPosition.getLatitude(),
                            previousPosition.getLongitude(), position.getLatitude(), position.getLongitude()));
                }
                previousPosition = position;
                speedSum += position.getSpeed();
                result.setMaxSpeed(position.getSpeed());
            }
            result.setAverageSpeed(speedSum / positions.size());
            result.setDistance(new BigDecimal(result.getDistance()).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }
        return result;
    }

    public static String getJson(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws SQLException {
        JsonObjectBuilder json = Json.createObjectBuilder();
        for (long deviceId: ReportUtils.getReportedDevices(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            json.add(String.valueOf(deviceId), JsonConverter.objectToJson(calculateGeneralResult(deviceId, from, to)));
        }
        return json.build().toString();
    }

    public static byte[] getCsv(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws SQLException {
        CsvBuilder csv = new CsvBuilder();
        for (long deviceId: ReportUtils.getReportedDevices(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            csv.addHeaderLine(new GeneralResult());
            csv.addLine(calculateGeneralResult(deviceId, from, to));
        }
        return csv.get();
    }
}
