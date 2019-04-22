package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.fueldataparsers.omnicomm;

import org.apache.commons.lang.StringUtils;
import org.traccar.model.Position;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.fueldataparsers.FuelDataParser;

import java.util.Optional;

public class DigitalLLSFuelDataParser implements FuelDataParser {

    // NOTE: with this type of sensorData field, we can only handle one sensor on this type of device.
    private static final int INVALID_FUEL_FREQUENCY = 0xFFF;
    private static final String SENSOR_DATA = "sensorData";
    private static final String FREQUENCY_PREFIX = "F=";
    private static final String FUEL_PART_PREFIX = "N=";

    @Override
    public Optional<Long> getFuelLevelPointsFromPayload(Position position, String fuelDataField) {

        String sensorDataString = (String) position.getAttributes().get(SENSOR_DATA);

        if (StringUtils.isBlank(sensorDataString)) {
            return Optional.empty();
        }

        String[] sensorDataParts = sensorDataString.split(" "); // Split on space to get the 3 parts
        String frequencyString = sensorDataParts[0];
        String temperatureString = sensorDataParts[1];
        String fuelLevelPointsString = sensorDataParts[2];

        if (StringUtils.isBlank(frequencyString)
                || StringUtils.isBlank(temperatureString)
                || StringUtils.isBlank(fuelLevelPointsString)) {

            return Optional.empty();
        }

        String[] frequencyParts = frequencyString.split(FREQUENCY_PREFIX);
        if (frequencyParts.length < 2) {
            return Optional.empty();
        }

        // If frequency is > xFFF (4096), it is invalid per the spec; so ignore it.
        Long frequency = Long.parseLong(frequencyParts[1], 16);
        if (frequency > INVALID_FUEL_FREQUENCY) {
            return Optional.empty();
        }

        String[] fuelParts = fuelLevelPointsString.split(FUEL_PART_PREFIX);
        if (fuelParts.length < 2) {
            return Optional.empty();
        }

        Long fuelSensorPoints = Long.parseLong(fuelParts[1].split("\\.")[0], 16);
        return Optional.of(fuelSensorPoints);
    }
}
