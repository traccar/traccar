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

    private static Map<String, String> gpsProtocolToFuelFieldMap = new ConcurrentHashMap<>();

    static {
        gpsProtocolToFuelFieldMap.put("aquila", Context.getConfig().getString("aquila.fuel_analog"));
        gpsProtocolToFuelFieldMap.put("teltonika", Context.getConfig().getString("teltonika.fuel_analog"));
    }

    @Override
    public Optional<Long> getFuelLevelPointsFromPayload(Position position) {

        String gpsDeviceProtocol = position.getProtocol();
        if (StringUtils.isBlank(gpsDeviceProtocol)) {
            Log.debug(String.format(
                    "Invalid protocol on payload for deviceId: %s", position.getDeviceId()));
            return Optional.empty();
        }

        String fuelDataFieldName = gpsProtocolToFuelFieldMap.getOrDefault(gpsDeviceProtocol, null);

        if (StringUtils.isBlank(fuelDataFieldName)) {
            Log.debug(String.format(
                    "Fuel data not found on payload for %s, on %s", gpsDeviceProtocol, fuelDataFieldName));
            return Optional.empty();
        }

        return Optional.of(((Number) position.getAttributes().get(fuelDataFieldName)).longValue());
    }
}
