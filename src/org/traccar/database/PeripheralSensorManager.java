package org.traccar.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.traccar.helper.Log;
import org.traccar.model.PeripheralSensor;

import javax.json.Json;
import javax.json.JsonObject;
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

    private final Map<String, String> deviceSensorToCalibrationDataMap =
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
                    deviceSensorToCalibrationDataMap.put(p.getDeviceId() + "_" + p.getPeripheralSensorId(), p.getCalibrationData());
                }
                Log.info("Created linked peripheral devices info: " + deviceToPeripheralSensorMap.size());
            } catch (SQLException e) {
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

    // Remove this function when ready to commit
    public static Optional<List<PeripheralSensor>> getMockList() {
        PeripheralSensor p = new PeripheralSensor();
        p.setPeripheralSensorId(1);
        ArrayList l = new ArrayList();
        l.add(p);
        return Optional.of(l);
    }
}
