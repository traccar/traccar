package org.traccar.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class WifiAccessPointTest {

    private WifiAccessPoint createWifiAccessPoint() {
        WifiAccessPoint wifiAccessPoint = WifiAccessPoint.from("00:11:22:33:44:55", 100);
        wifiAccessPoint.setChannel(100);
        return wifiAccessPoint;
    }

    @Test
    @DisplayName("Channel is set correctly")
    void testChannelIsSetCorrectly() {
        WifiAccessPoint wifiAccessPoint = createWifiAccessPoint();
        assertEquals(100, wifiAccessPoint.getChannel());
    }

    @Test
    @DisplayName("A wifi access point is equal to itself")
    void testWifiAccessPointIsEqualToItself() {
        WifiAccessPoint wifiAccessPoint = createWifiAccessPoint();
        assertEquals(wifiAccessPoint, wifiAccessPoint);
    }

    @Test
    @DisplayName("A wifi access point is not equal to null")
    void testWifiAccessPointIsNotEqualToNull() {
        WifiAccessPoint wifiAccessPoint = createWifiAccessPoint();
        assertFalse(wifiAccessPoint.equals(null));
    }

    @Test
    @DisplayName("A wifi access point is not equal to another object")
    void testWifiAccessPointIsNotEqualToAnotherObject() {
        WifiAccessPoint wifiAccessPoint = createWifiAccessPoint();
        Position position = new Position();
        assertFalse(wifiAccessPoint.equals(position));
    }

    @Test
    @DisplayName("Different MAC address makes wifi access point unequal")
    void testDifferentMacAddressMakeWifiAccessPointUnequal() {
        WifiAccessPoint wifiAccessPoint1 = createWifiAccessPoint();
        WifiAccessPoint wifiAccessPoint2 = createWifiAccessPoint();
        wifiAccessPoint2.setMacAddress("00:11:22:33:44:56");
        assertNotEquals(wifiAccessPoint1, wifiAccessPoint2);
    }

    @Test
    @DisplayName("Different signal strength makes wifi access point unequal")
    void testDifferentSignalStrengthMakeWifiAccessPointUnequal() {
        WifiAccessPoint wifiAccessPoint1 = createWifiAccessPoint();
        WifiAccessPoint wifiAccessPoint2 = createWifiAccessPoint();
        wifiAccessPoint2.setSignalStrength(101);
        assertNotEquals(wifiAccessPoint1, wifiAccessPoint2);
    }

    @Test
    @DisplayName("Different channel makes wifi access point unequal")
    void testDifferentChannelMakeWifiAccessPointUnequal() {
        WifiAccessPoint wifiAccessPoint1 = createWifiAccessPoint();
        WifiAccessPoint wifiAccessPoint2 = createWifiAccessPoint();
        wifiAccessPoint2.setChannel(101);
        assertNotEquals(wifiAccessPoint1, wifiAccessPoint2);
    }
}
