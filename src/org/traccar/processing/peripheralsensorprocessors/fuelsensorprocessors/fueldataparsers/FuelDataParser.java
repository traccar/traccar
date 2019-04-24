package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.fueldataparsers;

import org.traccar.model.Position;

import java.util.Optional;

public interface FuelDataParser {

    Optional<Long> getFuelLevelPointsFromPayload(Position position, String fuelDataField);
}
