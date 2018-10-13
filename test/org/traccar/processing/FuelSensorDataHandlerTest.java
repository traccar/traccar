package org.traccar.processing;

import org.junit.Test;
import org.traccar.model.Position;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.FuelActivity;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.FuelSensorDataHandler;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.FuelEventMetadata;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.FuelSensorDataHandlerHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FuelSensorDataHandlerTest {


    public void testFuelFillActivity() {

        int sensorId = 1;

        FuelSensorDataHandler fuelSensorDataHandler =
                new FuelSensorDataHandler(false);

        // Fuel is getting consumed before and after we fill.
        List<Position> deviceBeforeFillPositions = generatePositions(10, 40, 30);
        List<Position> deviceFillPositions = generatePositions(10, 30, 70);
        List<Position> deviceAfterFillPositions = generatePositions(10, 70, 65);

        deviceBeforeFillPositions.addAll(deviceFillPositions);
        deviceBeforeFillPositions.addAll(deviceAfterFillPositions);

        Map<String, FuelEventMetadata> fuelEventMetadataMap = new ConcurrentHashMap<>();

        double threshold = 5.34;

        List<FuelActivity> activities = new LinkedList<>();
        for (int start = 0, end = 9; end < deviceBeforeFillPositions.size(); start++, end++) {
            List<Position> subListToPass = deviceBeforeFillPositions.subList(start, end);
            activities.add(fuelSensorDataHandler.checkForActivity(subListToPass, fuelEventMetadataMap,
                                                                  sensorId, threshold));
        }

        int fuelFills = 0;
        for (FuelActivity activity : activities) {
            if (activity.getActivityType() == FuelActivity.FuelActivityType.FUEL_FILL) {
                fuelFills++;
            }
        }

        assert fuelFills == 1;
    }


    public void testFuelDrainActivity() {

        int sensorId = 1;

        FuelSensorDataHandler fuelSensorDataHandler =
                new FuelSensorDataHandler(false);

        // Fuel is getting consumed before and after we fill.
        List<Position> deviceBeforeDrainPositions = generatePositions(20, 80, 60);
        List<Position> deviceDrainPositions = generatePositions(10, 60, 50);
        List<Position> deviceAfterDrainPositions = generatePositions(20, 50, 45);

        deviceBeforeDrainPositions.addAll(deviceDrainPositions);
        deviceBeforeDrainPositions.addAll(deviceAfterDrainPositions);

        Map<String, FuelEventMetadata> fuelEventMetadataMap = new ConcurrentHashMap<>();

        double threshold = 3;

        List<FuelActivity> activities = new LinkedList<>();
        for (int start = 0, end = 9; end < deviceBeforeDrainPositions.size(); start++, end++) {
            List<Position> subListToPass = deviceBeforeDrainPositions.subList(start, end);
            activities.add(fuelSensorDataHandler.checkForActivity(subListToPass, fuelEventMetadataMap,
                                                                  sensorId, threshold));
        }

        int fuelDrains = 0;
        for (FuelActivity activity : activities) {
            if (activity.getActivityType() == FuelActivity.FuelActivityType.FUEL_DRAIN) {
                fuelDrains++;
            }
        }

        assert fuelDrains == 1;
    }

    private List<Position> generatePositions(int size,
                                             double startFuelLevel,
                                             double endFuelLevel) {

        List<Position> positions = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Position position = new Position();
            double fuelIncrement = i * ((endFuelLevel - startFuelLevel)/size);
            double fuelValue = startFuelLevel + fuelIncrement;
            position.set(Position.KEY_FUEL_LEVEL, fuelValue);
            position.set(Position.KEY_CALIBRATED_FUEL_LEVEL, fuelValue);
            position.setDeviceTime(getAdjustedTime(30 *(size - i)));
            positions.add(position);
        }

        return positions;
    }

    private Position getPositionWithCalibValue(double value) {
        Position p = new Position();
        p.set(Position.KEY_CALIBRATED_FUEL_LEVEL, value);
        return p;
    }

    @Test
    public void testOutliers() {
        List<Position> positions = new ArrayList<>();
        positions.add(getPositionWithCalibValue(100.0));
        positions.add(getPositionWithCalibValue(100.2));
        positions.add(getPositionWithCalibValue(100.5));
        positions.add(getPositionWithCalibValue(100.3));
        positions.add(getPositionWithCalibValue(102.0));
        positions.add(getPositionWithCalibValue(100.1));
        positions.add(getPositionWithCalibValue(100.4));
        positions.add(getPositionWithCalibValue(100.2));
        positions.add(getPositionWithCalibValue(100.6));

        boolean isOutlier = FuelSensorDataHandlerHelper.isOutlierPresentInSublist(positions,
                                                              4, Optional.of(100L));

        assert isOutlier == true;
    }

    private Date getAdjustedTime(int secondsBehind) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -secondsBehind);
        return calendar.getTime();
    }
}