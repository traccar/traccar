package org.traccar.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DeviceTest {

    private Device createDevice() {
        Device device = new Device();
        device.setUniqueId("ID-1");
        device.setName("Test Device");
        return device;
    }

    @ParameterizedTest
    @DisplayName("Throws exception when unique ID is invalid format")
    @ValueSource(strings = {"/iphone-device", "12345/67890", "invalid/device/id", "Iphone\\mobile"})
    void throwsExceptionWhenUniqueIdIsInvalidFormat(String uniqueId) {
        assertThrows(IllegalArgumentException.class, () -> new Device().setUniqueId(uniqueId));
    }

    @Test
    @DisplayName("Null status returns STATUS_OFFLINE")
    void nullStatusReturnsStatusOffline() {
        Device device = createDevice();
        device.setStatus(null);
        assertEquals(Device.STATUS_OFFLINE, device.getStatus());
    }

    @Test
    @DisplayName("Non null status returns the status")
    void nonNullStatusReturnsTheStatus() {
        Device device = createDevice();
        device.setStatus(Device.STATUS_ONLINE);
        assertEquals(Device.STATUS_ONLINE, device.getStatus());
    }

    @ParameterizedTest
    @DisplayName("setPhone set different phone names")
    @ValueSource(strings = {"iPhone", "Android", "Samsung", "One+"})
    void setPhoneSetDifferentPhoneNames(String name) {
        Device device = createDevice();
        device.setPhone(name);
        assertEquals(name, device.getPhone());
    }

    @Test
    @DisplayName("Null phone returns null")
    void nullPhoneReturnsNull() {
        Device device = createDevice();
        device.setPhone(null);
        assertNull(device.getPhone());
    }
}
