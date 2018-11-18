package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import org.traccar.BaseDataHandler;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.PeripheralSensor;
import org.traccar.model.Position;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.fueldataparsers.FuelDataParser;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.fueldataparsers.omnicomm.AnalogLLSFuelDataParser;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.fueldataparsers.omnicomm.DigitalLLSFuelDataParser;
import org.traccar.transforms.model.SensorPointsMap;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class FuelDataCalibrationHandler extends BaseDataHandler {

    private final static Map<String, TreeMap<Long, SensorPointsMap>> deviceToSensorCalibrationPointsMap =
            new ConcurrentHashMap<>();

    private final static Map<String, FuelDataParser> FUEL_SENOR_TYPE_PARSER_MAP = new ConcurrentHashMap<>();

    static {
        FUEL_SENOR_TYPE_PARSER_MAP.put("analog", new AnalogLLSFuelDataParser());
        FUEL_SENOR_TYPE_PARSER_MAP.put("digital", new DigitalLLSFuelDataParser());
    }

    @Override
    protected Position handlePosition(Position position) {

        if (position.getAttributes().containsKey(Position.KEY_CALIBRATED_FUEL_LEVEL)) {
            return position;
        }

        long deviceId = position.getDeviceId();
        Optional<PeripheralSensor> sensorOnDevice = Context.getPeripheralSensorManager().getSensorByDeviceId(deviceId);

        if (!sensorOnDevice.isPresent()) {
            Log.debug(String.format("No sensor found on deviceId: %d. Refreshing sensors map.", deviceId));
            Context.getPeripheralSensorManager().refreshPeripheralSensorsMap();
            return position;
        }

        String fuelSensorType = sensorOnDevice.get().getTypeName().split("_")[1].toLowerCase();

        FuelDataParser parser = FUEL_SENOR_TYPE_PARSER_MAP.get(fuelSensorType);
        Optional<Long> fuelLevelPoints = parser.getFuelLevelPointsFromPayload(position);

        long sensorId = sensorOnDevice.get().getPeripheralSensorId();

        if (!fuelLevelPoints.isPresent()) {
            Log.debug(String.format("No fuel data found on payload for %d and sensor %d", deviceId, sensorId));
            return position;
        }

        Optional<Double> maybeCalibratedData =
                getCalibratedFuelLevel(deviceId, sensorId, fuelLevelPoints.get());

        if (!maybeCalibratedData.isPresent()) {
            Log.debug(String.format("Calibrated fuel level could not be determined for device %d and sensor %d",
                                    deviceId, sensorId));

            return position;
        }

        position.set(Position.KEY_CALIBRATED_FUEL_LEVEL, maybeCalibratedData.get());
        return position;
    }

    private Optional<Double> getCalibratedFuelLevel(Long deviceId, Long sensorId, Long sensorFuelLevelPoints) {

        Optional<TreeMap<Long, SensorPointsMap>> maybeSensorPointsToVolumeMap =
                Context.getPeripheralSensorManager()
                       .getSensorCalibrationPointsMap(deviceId, sensorId);

        if (!maybeSensorPointsToVolumeMap.isPresent()) {
            return Optional.empty();
        }

        TreeMap<Long, SensorPointsMap> sensorPointsToVolumeMap = maybeSensorPointsToVolumeMap.get();
        Map.Entry<Long, SensorPointsMap> max = sensorPointsToVolumeMap.lastEntry();
        Map.Entry<Long, SensorPointsMap> min = sensorPointsToVolumeMap.firstEntry();

        if (sensorFuelLevelPoints > max.getKey()
                || sensorFuelLevelPoints < min.getKey()) {
            return Optional.empty();
        }

        Map.Entry<Long, SensorPointsMap> previous = sensorPointsToVolumeMap.floorEntry(sensorFuelLevelPoints);
        Map.Entry<Long, SensorPointsMap> next = sensorPointsToVolumeMap.ceilingEntry(sensorFuelLevelPoints);


        if (next == null && sensorFuelLevelPoints.equals(max.getKey())) {
            next = max;
        }

        if (previous == null && sensorFuelLevelPoints.equals(min.getKey())) {
            previous = min;
        }

        SensorPointsMap previousFuelLevelInfo = previous.getValue();
        SensorPointsMap nextFuelLevelInfo = next.getValue();

        try {
            double currentAveragePointsPerLitre = nextFuelLevelInfo.getPointsPerLitre();
            long previousPoint = sensorPointsToVolumeMap.floorKey(sensorFuelLevelPoints);
            long previousFuelLevel = previousFuelLevelInfo.getFuelLevel();

            double fuelLevel = currentAveragePointsPerLitre > 0.0
                    ? ((sensorFuelLevelPoints - previousPoint) / currentAveragePointsPerLitre) + previousFuelLevel
                    : 0.0;

            return Optional.of(fuelLevel);
        } catch (Exception ex) {
            Log.debug("Null nextFuelLevelInfo - deviceID: " + deviceId
                              + " sensorId: " + sensorId
                              + " reported sensorFuelLevelPoints" + sensorFuelLevelPoints);
        }

        return Optional.empty();
    }
}
