package org.traccar.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PositionBoundaryValueAnalysisTest {
    private final Position position = new Position();

    @ParameterizedTest
    @DisplayName("Valid latitude is accepted")
    @ValueSource(doubles = {-90, -89.9, 89.9, 90})
    void validLatitude(double latitude) {
        position.setLatitude(latitude);
        assertEquals(latitude, position.getLatitude());
    }

    @ParameterizedTest
    @DisplayName("Invalid latitude is rejected")
    @ValueSource(doubles = {-90.1, 90.1})
    void invalidLatitude(double latitude) {
        assertThrows(IllegalArgumentException.class, () -> position.setLatitude(latitude));
    }

    @ParameterizedTest
    @DisplayName("Valid longitude is accepted")
    @ValueSource(doubles = {-180, -179.9, 179.9, 180})
    void validLongitude(double longitude) {
        position.setLongitude(longitude);
        assertEquals(longitude, position.getLongitude());
    }

    @ParameterizedTest
    @DisplayName("Invalid longitude is rejected")
    @ValueSource(doubles = {-180.1, 180.1})
    void invalidLongitude(double longitude) {
        assertThrows(IllegalArgumentException.class, () -> position.setLongitude(longitude));
    }
}
