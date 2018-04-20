package org.traccar.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.traccar.helper.Log;
import org.traccar.model.PeripheralSensor;
import org.traccar.transforms.model.FuelSensorCalibration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
                    FuelSensorCalibration fuelSensorCalibration = calibrationDataMapper.readValue(p.getCalibrationData(), FuelSensorCalibration.class);
                    deviceSensorToCalibrationDataMap.put(buildDeviceSensorMapKey(p.getDeviceId(), p.getPeripheralSensorId()), fuelSensorCalibration);
                }
                Log.info("Created linked peripheral devices info: " + deviceToPeripheralSensorMap.size());
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

    private String buildDeviceSensorMapKey(long deviceId, long sensorId) {
        return deviceId + "_" + sensorId;
    }

}
