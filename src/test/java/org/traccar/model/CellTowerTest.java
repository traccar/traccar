package org.traccar.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CellTowerTest {

    private CellTower createCellTower() {
        CellTower cellTower = CellTower.from(240, 4, 1061, 2000);
        cellTower.setRadioType("UMTS");
        cellTower.setSignalStrength(10);
        return cellTower;
    }

    @Test
    @DisplayName("Signal strength is null")
    void signalStrengthIsNull() {
        CellTower cellTower = createCellTower();
        cellTower.setSignalStrength(null);
        assertNull(cellTower.getSignalStrength());
    }

    @Test
    @DisplayName("A cell tower is equal to itself")
    void equalToItself() {
        CellTower cellTower = createCellTower();
        assertEquals(cellTower, cellTower);
    }

    @Test
    @DisplayName("A cell tower is equal to another cell tower with the same properties")
    void equalToAnotherCellTower() {
        CellTower cellTower1 = createCellTower();
        CellTower cellTower2 = createCellTower();
        assertEquals(cellTower1, cellTower2);
    }

    @Test
    @DisplayName("A cell tower is not equal to null")
    void notEqualToNull() {
        CellTower cellTower = createCellTower();
        assertFalse(cellTower.equals(null));
    }

    @Test
    @DisplayName("A cell tower is not equal to an object of another type")
    void notEqualToAnotherType() {
        CellTower cellTower = createCellTower();
        Position position = new Position();
        assertFalse(cellTower.equals(position));
    }

    @Test
    @DisplayName("Different radio type makes cell towers unequal")
    void differentRadioTypeMakesCellTowersUnequal() {
        CellTower cellTower1 = createCellTower();
        CellTower cellTower2 = createCellTower();
        cellTower2.setRadioType("LTE");
        assertNotEquals(cellTower1, cellTower2);
    }

    @Test
    @DisplayName("Different cellId makes cell towers unequal")
    void differentCellIdMakesCellTowersUnequal() {
        CellTower cellTower1 = createCellTower();
        CellTower cellTower2 = createCellTower();
        cellTower2.setCellId(3000L);
        assertNotEquals(cellTower1, cellTower2);
    }

    @Test
    @DisplayName("Different location area code makes cell towers unequal")
    void differentLocationAreaCodeMakesCellTowersUnequal() {
        CellTower cellTower1 = createCellTower();
        CellTower cellTower2 = createCellTower();
        cellTower2.setLocationAreaCode(1062);
        assertNotEquals(cellTower1, cellTower2);
    }

    @Test
    @DisplayName("Different home country code makes cell towers unequal")
    void differentHomeCountryCodeMakesCellTowersUnequal() {
        CellTower cellTower1 = createCellTower();
        CellTower cellTower2 = createCellTower();
        cellTower2.setMobileCountryCode(241);
        assertNotEquals(cellTower1, cellTower2);
    }

    @Test
    @DisplayName("Different home network code makes cell towers unequal")
    void differentHomeNetworkCodeMakesCellTowersUnequal() {
        CellTower cellTower1 = createCellTower();
        CellTower cellTower2 = createCellTower();
        cellTower2.setMobileNetworkCode(5);
        assertNotEquals(cellTower1, cellTower2);
    }

    @Test
    @DisplayName("Different signal strength makes cell towers unequal")
    void differentSignalStrengthMakesCellTowersUnequal() {
        CellTower cellTower1 = createCellTower();
        CellTower cellTower2 = createCellTower();
        cellTower2.setSignalStrength(5);
        assertNotEquals(cellTower1, cellTower2);
    }
}
