package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import org.traccar.BaseDataHandler;
import org.traccar.Context;
import org.traccar.model.PeripheralSensor;
import org.traccar.model.Position;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.fueldataparsers.FuelDataParser;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.fueldataparsers.omnicomm.AnalogLLSFuelDataParser;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.fueldataparsers.omnicomm.DigitalLLSFuelDataParser;
import org.traccar.transforms.model.SensorPointsMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FuelDataCalibrationHandler extends BaseDataHandler {

    private final static Map<String, FuelDataParser> FUEL_SENOR_TYPE_PARSER_MAP = new ConcurrentHashMap<>();

    static {
        FUEL_SENOR_TYPE_PARSER_MAP.put("analog", new AnalogLLSFuelDataParser());
        FUEL_SENOR_TYPE_PARSER_MAP.put("digital", new DigitalLLSFuelDataParser());
    }

    @Override
    protected Position handlePosition(Position position) {
        long deviceId = position.getDeviceId();
        Optional<List<PeripheralSensor>> sensorsOnDevice = Context.getPeripheralSensorManager().getLinkedPeripheralSensors(deviceId);

        if (!sensorsOnDevice.isPresent() || sensorsOnDevice.get().isEmpty()) {
            FuelSensorDataHandlerHelper.logDebugIfDeviceId(String.format("No sensors found on deviceId: %d. Refreshing sensors map.", deviceId), deviceId);
            Context.getPeripheralSensorManager().refreshPeripheralSensorsMap();
            return position;
        }

        List<PeripheralSensor> fuelSensorsList = sensorsOnDevice.get();
        if (fuelSensorsList.isEmpty()) {
            FuelSensorDataHandlerHelper.logDebugIfDeviceId(String.format("Sensors list empty for deviceId: %d. Refreshing sensors map.", deviceId), deviceId);
            return position;
        }

        for (PeripheralSensor fuelSensor : fuelSensorsList) {
            String calibFuelField = fuelSensor.getCalibFuelFieldName();
            if (position.getAttributes().containsKey(calibFuelField)) {
                continue;
            }
            handleCalibrationData(position, deviceId, fuelSensor);
        }

        ProcessingInfo processingInfo = Context.getDeviceManager().getDeviceProcessingInfo(deviceId);
        String fuelProcessingType = processingInfo.getProcessingType();

        switch (fuelProcessingType) {
            case ProcessingInfo.AVG_FUEL_PROCESS_TYPE:
            case ProcessingInfo.SUM_FUEL_PROCESS_TYPE:
                relatedSensorDataProcessing(position, fuelSensorsList, processingInfo);
                break;
            default:
                break;
        }

        return position;
    }

    private void relatedSensorDataProcessing(Position position,
                                             List<PeripheralSensor> sensorsOnDeviceList,
                                             ProcessingInfo processingInfo) {

        // NOTE: we'll need to ensure that the calib_fuel field names on each sensor in this case are different from the
        // default "calib_fuel" and different from each other as well.

        String fuelProcessingType = processingInfo.getProcessingType();
        DoubleSummaryStatistics calibFuelValues = sensorsOnDeviceList.stream()
                                                                     .map(PeripheralSensor::getCalibFuelFieldName)
                                                                     .map(position::getDouble)
                                                                     .mapToDouble(Double::valueOf)
                                                                     .summaryStatistics();

        switch(fuelProcessingType) {
            case ProcessingInfo.AVG_FUEL_PROCESS_TYPE:
                position.set(processingInfo.getFinalCalibFieldName(), calibFuelValues.getAverage());
                break;
            case ProcessingInfo.SUM_FUEL_PROCESS_TYPE:
                position.set(processingInfo.getFinalCalibFieldName(), calibFuelValues.getSum());
                break;
        }
    }

    private void handleCalibrationData(Position position, long deviceId, PeripheralSensor fuelSensor) {

        String fuelSensorType = fuelSensor.getTypeName().split("_")[1].toLowerCase();
        FuelDataParser parser = FUEL_SENOR_TYPE_PARSER_MAP.get(fuelSensorType);
        Optional<Long> fuelLevelPoints =
                parser.getFuelLevelPointsFromPayload(position, fuelSensor.getSensorFuelDataField(position));

        long sensorId = fuelSensor.getPeripheralSensorId();

        if (!fuelLevelPoints.isPresent()) {
            FuelSensorDataHandlerHelper.logDebugIfDeviceId(String.format("No fuel data found on payload for %d and sensor %d", deviceId, sensorId), deviceId);
            return;
        }

        Optional<Double> maybeCalibratedData =
                getCalibratedFuelLevel(deviceId, sensorId, fuelLevelPoints.get());

        if (!maybeCalibratedData.isPresent()) {
            FuelSensorDataHandlerHelper.logDebugIfDeviceId(String.format("Calibrated fuel level could not be determined for device %d and sensor %d",
                                    deviceId, sensorId), deviceId);

            return;
        }

        position.set(fuelSensor.getCalibFuelFieldName(), maybeCalibratedData.get());
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


        if (next == null && sensorFuelLevelPoints >= max.getKey()) {
            next = max;
        }

        if (previous == null && sensorFuelLevelPoints <= min.getKey()) {
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
            FuelSensorDataHandlerHelper.logDebugIfDeviceId("Null nextFuelLevelInfo - deviceID: " + deviceId
                              + " sensorId: " + sensorId
                              + " reported sensorFuelLevelPoints" + sensorFuelLevelPoints, deviceId);
        }

        return Optional.empty();
    }
}
