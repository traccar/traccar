package org.traccar.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class NetworkTest {

    private Network createNetwork() {
        Network network = new Network(CellTower.from(240, 4, 1061, 2000));
        network.setHomeMobileCountryCode(240);
        network.setHomeMobileNetworkCode(4);
        network.setCarrier("Telia");
        network.setRadioType("UMTS");
        network.addWifiAccessPoint(WifiAccessPoint.from("00:11:22:33:44:55", -60));
        return network;
    }

    @Test
    @DisplayName("Network values are set correct")
    void networkValuesAreSetCorrect() {
        Network network = createNetwork();
        assertEquals(240, network.getHomeMobileCountryCode());
        assertEquals(4, network.getHomeMobileNetworkCode());
        assertEquals("Telia", network.getCarrier());
        assertEquals("UMTS", network.getRadioType());
        assertEquals(1, network.getWifiAccessPoints().size());
    }

    @Test
    @DisplayName("A network is equal to itself")
    void equalToItself() {
        Network network = createNetwork();
        assertEquals(network, network);
    }

    @Test
    @DisplayName("Networks with the same values are equal and have the same hashCode")
    void sameValuesAreEqual() {
        Network network1 = createNetwork();
        Network network2 = createNetwork();
        assertEquals(network1, network2);
        assertEquals(network1.hashCode(), network2.hashCode());
    }

    @Test
    @DisplayName("A network is not equal to null")
    void notEqualToNull() {
        Network network = createNetwork();
        assertFalse(network.equals(null));
    }

    @Test
    @DisplayName("A network is not equal to an object of another type")
    void notEqualToAnotherType() {
        Network network = createNetwork();
        Position position = new Position();
        assertFalse(network.equals(position));
    }

    @Test
    @DisplayName("Different home country code makes networks unequal")
    void differentHomeCountryCodeMakesNetworksUnequal() {
        Network network1 = createNetwork();
        Network network2 = createNetwork();
        network2.setHomeMobileCountryCode(241);
        assertNotEquals(network1, network2);
    }

    @Test
    @DisplayName("Different home network code makes networks unequal")
    void differentHomeNetworkCodeMakesNetworksUnequal() {
        Network network1 = createNetwork();
        Network network2 = createNetwork();
        network2.setHomeMobileNetworkCode(5);
        assertNotEquals(network1, network2);
    }

    @Test
    @DisplayName("Different radio type makes networks unequal")
    void differentRadioTypeMakesNetworksUnequal() {
        Network network1 = createNetwork();
        Network network2 = createNetwork();
        network2.setRadioType("LTE");
        assertNotEquals(network1, network2);
    }

    @Test
    @DisplayName("Different carrier makes networks unequal")
    void differentCarrierMakesNetworksUnequal() {
        Network network1 = createNetwork();
        Network network2 = createNetwork();
        network2.setCarrier("Telenor");
        assertNotEquals(network1, network2);
    }

    @Test
    @DisplayName("Different cell towers make networks unequal")
    void differentCellTowersMakesNetworksUnequal() {
        Network network1 = createNetwork();
        Network network2 = createNetwork();
        network2.addCellTower(CellTower.from(240, 1, 55, 3000));
        assertNotEquals(network1, network2);
    }

    @Test
    @DisplayName("Different wifi access points make networks unequal")
    void differentWifiAccessPointsMakesNetworksUnequal() {
        Network network1 = createNetwork();
        Network network2 = createNetwork();
        network2.addWifiAccessPoint(WifiAccessPoint.from("00:11:22:33:44:66", -60));
        assertNotEquals(network1, network2);
    }
}
