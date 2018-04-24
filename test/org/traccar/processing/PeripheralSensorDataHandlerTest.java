package org.traccar.processing;

import javafx.geometry.Pos;
import org.junit.Test;
import org.traccar.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeripheralSensorDataHandlerTest {

    @Test
    public void testActivity() {
        PeripheralSensorDataHandler peripheralSensorDataHandler =
                new PeripheralSensorDataHandler();

        // Fuel is getting consumed before and after we fill.
        List<Position> deviceBeforeFillPositions = generatePositions(10, 40, 30);
        List<Position> deviceFillPositions = generatePositions(10, 30, 70);
        List<Position> deviceAfterFillPositions = generatePositions(10, 70, 65);

        deviceBeforeFillPositions.addAll(deviceFillPositions);
        deviceBeforeFillPositions.addAll(deviceAfterFillPositions);

        Map<Long, PeripheralSensorDataHandler.FuelEventMetadata> fuelEventMetadataMap = new ConcurrentHashMap<>();

        double threshold = 5.31;

        for (int start = 0, end = 9; end < deviceBeforeFillPositions.size(); start++, end++) {
            List<Position> subListToPass = deviceBeforeFillPositions.subList(start, end);
            subListToPass.forEach(d -> System.out.print(d.getAttributes().get(Position.KEY_FUEL_LEVEL) + " "));
            System.out.println();
            peripheralSensorDataHandler.checkForActivity(subListToPass, fuelEventMetadataMap, threshold, 10);
        }
    }

    private List<Position> generatePositions(int size,
                                             double startFuelLevel,
                                             double endFuelLevel) {

        List<Position> positions = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Position position = new Position();
            double fuelIncrement = i * ((endFuelLevel - startFuelLevel)/size);
            position.set(Position.KEY_FUEL_LEVEL, startFuelLevel + fuelIncrement);
            positions.add(position);
        }

        return positions;
    }




}