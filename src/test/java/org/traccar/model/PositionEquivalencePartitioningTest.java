package org.traccar.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PositionEquivalencePartitioningTest {
    private final Position position = new Position();

    @ParameterizedTest
    @DisplayName("Valid input for latitude is accepted")
    @ValueSource(doubles = {-45, 0.0, 45})
    void validLatitude(double latitude) {
        position.setLatitude(latitude);
        assertEquals(latitude, position.getLatitude());
    }

    @ParameterizedTest
    @DisplayName("Invalid input for latitude is rejected")
    @ValueSource(doubles = {-91, 91, 180})
    void invalidLatitude(double latitude) {
        assertThrows(IllegalArgumentException.class, () -> position.setLatitude(latitude));
    }

    @ParameterizedTest
    @DisplayName("Valid input for longitude is accepted")
    @ValueSource(doubles = {-45, 0.0, 45})
    void validLongitude(double longitude) {
        position.setLongitude(longitude);
        assertEquals(longitude, position.getLongitude());
    }

    @ParameterizedTest
    @DisplayName("Invalid input for longitude is rejected")
    @ValueSource(doubles = {-181, 181})
    void invalidLongitude(double longitude) {
        assertThrows(IllegalArgumentException.class, () -> position.setLongitude(longitude));
    }

    @Test
    @DisplayName("setGeofenceIds handles an empty-list correct")
    void setGeofenceIdsHandlesEmptyListCorrect() {
        position.setGeofenceIds(new ArrayList<>());
        assertTrue(position.getGeofenceIds().isEmpty());
    }

    @Test
    @DisplayName("setGeofenceIds handles a non-empty-list correct")
    void setGeofenceIdsHandlesNonEmptyListCorrect() {
        List<Long> geofenceIds = Arrays.asList(1L, 2L, 3L);
        position.setGeofenceIds(geofenceIds);
        assertEquals(geofenceIds, position.getGeofenceIds());
    }

    @Test
    @DisplayName("setGeofenceIds handles null list correct")
    void setGeofenceIdsHandlesNullListCorrect() {
        position.setGeofenceIds(null);
        assertNull(position.getGeofenceIds());
    }
}
