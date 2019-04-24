package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.fueldataparsers.omnicomm;

import org.apache.commons.lang.StringUtils;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Position;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.fueldataparsers.FuelDataParser;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AnalogLLSFuelDataParser implements FuelDataParser {

    @Override
    public Optional<Long> getFuelLevelPointsFromPayload(Position position, String fuelDataFieldName) {

        String gpsDeviceProtocol = position.getProtocol();
        if (StringUtils.isBlank(gpsDeviceProtocol)) {
            Log.debug(String.format(
                    "Invalid protocol on payload for deviceId: %s", position.getDeviceId()));
            return Optional.empty();
        }

        if (StringUtils.isBlank(fuelDataFieldName) || !position.getAttributes().containsKey(fuelDataFieldName)) {
            Log.debug(String.format(
                    "Fuel data not found on payload for %s, on %s", gpsDeviceProtocol, fuelDataFieldName));
            return Optional.empty();
        }

        return Optional.of(((Number) position.getAttributes().get(fuelDataFieldName)).longValue());
    }
}
