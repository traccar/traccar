package org.traccar.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.PeripheralSensor;
import org.traccar.transforms.model.FuelSensorCalibration;
import org.traccar.transforms.model.SensorPointsMap;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PeripheralSensorManager extends ExtendedObjectManager<PeripheralSensor> {

    private final Map<Long, List<PeripheralSensor>> deviceToPeripheralSensorMap =
            new ConcurrentHashMap();

    private final Map<String, FuelSensorCalibration> deviceSensorToCalibrationDataMap =
            new ConcurrentHashMap<>();

    public PeripheralSensorManager(DataManager dataManager) {
        super(dataManager, PeripheralSensor.class);
        refreshPeripheralSensorsMap();
    }

    public void refreshPeripheralSensorsMap() {

        deviceToPeripheralSensorMap.clear();
        deviceSensorToCalibrationDataMap.clear();

        if (getDataManager() != null) {
            try {
                Collection<PeripheralSensor> peripheralSensors = getDataManager().getPeripheralSensors();
                for (PeripheralSensor p : peripheralSensors) {
                    List linkedPeripheralSensors = deviceToPeripheralSensorMap.get(p.getDeviceId());
                    if (linkedPeripheralSensors == null) {
                        linkedPeripheralSensors = new ArrayList();
                    }
                    linkedPeripheralSensors.add(p);
                    deviceToPeripheralSensorMap.put(p.getDeviceId(), linkedPeripheralSensors);

                    ObjectMapper calibrationDataMapper = new ObjectMapper();
                    FuelSensorCalibration fuelSensorCalibration =
                            calibrationDataMapper.readValue(p.getCalibrationData(),
                                                            FuelSensorCalibration.class);
                    deviceSensorToCalibrationDataMap.put(buildDeviceSensorMapKey(p.getDeviceId(),
                                                                                 p.getPeripheralSensorId()),
                                                         fuelSensorCalibration);
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Optional<List<PeripheralSensor>> getLinkedPeripheralSensors(long deviceId) {
        if (deviceToPeripheralSensorMap.containsKey(deviceId)) {
            return Optional.of(deviceToPeripheralSensorMap.get(deviceId));
        }

        return Optional.empty();
    }

    public Optional<FuelSensorCalibration> getDeviceSensorCalibrationData(long deviceId, long peripheralSensorId) {
        String lookupKey = buildDeviceSensorMapKey(deviceId, peripheralSensorId);

        if (deviceSensorToCalibrationDataMap.containsKey(lookupKey)) {
            return Optional.of(deviceSensorToCalibrationDataMap.get(lookupKey));
        }

        return Optional.empty();
    }

    public Optional<PeripheralSensor> getSensorByDeviceId(long deviceId) {
        // Note: Handles only one sensor per  gps device for now.

        Optional<List<PeripheralSensor>> sensorOnDevice = getLinkedPeripheralSensors(deviceId);

        if (!sensorOnDevice.isPresent()) {
            return Optional.empty();
        }

        return sensorOnDevice.get().stream().findFirst();
    }

    public Optional<TreeMap<Long, SensorPointsMap>> getSensorCalibrationPointsMap(long deviceId, long peripheralSensorId) {

        Optional<FuelSensorCalibration> fuelSensorCalibration = getDeviceSensorCalibrationData(deviceId, peripheralSensorId);

        if (!fuelSensorCalibration.isPresent()) {
            return Optional.empty();
        }

        // Make a B-tree map of the points to fuel level map
        TreeMap<Long, SensorPointsMap> sensorPointsToVolumeMap =
                new TreeMap<>(fuelSensorCalibration.get().getSensorPointsMap());

        return Optional.of(sensorPointsToVolumeMap);
    }

    public Optional<Long> getFuelTankMaxCapacity(Long deviceId, Long sensorId) {

        Optional<TreeMap<Long, SensorPointsMap>> maybeSensorPointsToVolumeMap =
                getSensorCalibrationPointsMap(deviceId, sensorId);

        if (!maybeSensorPointsToVolumeMap.isPresent()) {
            return Optional.empty();
        }

        TreeMap<Long, SensorPointsMap> sensorPointsToVolumeMap = maybeSensorPointsToVolumeMap.get();

        long lastFuelLevelKey = sensorPointsToVolumeMap.lastKey();
        return Optional.of(sensorPointsToVolumeMap.ceilingEntry(lastFuelLevelKey)
                                                  .getValue()
                                                  .getFuelLevel());
    }

    private String buildDeviceSensorMapKey(long deviceId, long sensorId) {
        return deviceId + "_" + sensorId;
    }

}
