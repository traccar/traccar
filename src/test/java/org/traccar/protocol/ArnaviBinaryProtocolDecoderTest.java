package org.traccar.protocol;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.CellTower;
import org.traccar.model.Position;

public class ArnaviBinaryProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testHeader1Decode() throws Exception {

        var decoder = inject(new ArnaviBinaryProtocolDecoder(null));

        verifyNull(decoder, binary(
                "ff22f30c45f5c90f0300"));

        verifyPositions(decoder, binary(
                "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d"),
                position("2017-07-07 05:09:55.000", true, 45.05597, 39.03347));
        }

    @Test
    public void testHeader2Decode() throws Exception {
            var decoder = inject(new ArnaviBinaryProtocolDecoder(null));
        verifyNull(decoder, binary(
                "ff23f30c45f5c90f0300"));
        verifyPositions(decoder, binary(
                "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d"),
                position("2017-07-07 05:09:55.000", true, 45.05597, 39.03347));
        }

    @Test
    public void testHeader3Decode() throws Exception {
            var decoder = inject(new ArnaviBinaryProtocolDecoder(null));
        verifyNull(decoder, binary("ff24f30c45f5c90f03000102030405060708"));
        verifyPositions(decoder, binary(
                "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d"),
                position("2017-07-07 05:09:55.000", true, 45.05597, 39.03347));
        }

    @Test
    public void testFullTagsAttributes() throws Exception {
            var decoder = inject(new ArnaviBinaryProtocolDecoder(null));

        verifyNull(decoder, binary("ff23ae52a969f30c45f5"));

                Object result = decoder.decode(null, null, binary(
                                "5bae01" + "9600" +
                                                "8a52a969" +
                                                "0373f35c42" +
                                                "049746ac42" +
                                                "0574147529" +
                                                "973e000000" +
                                                "07ca325cb0" +
                                                "61ca320000" +
                                                "0863fa001e" +
                                                "96e553fe00" +
                                                "6353801a02" +
                                                "01b50f5d6c" +
                                                "0601010000" +
                                                "474e000000" +
                                                "5b4e000000" +
                                                "5c00001700" +
                                                "5d00001700" +
                                                "3203000000" +
                                                "3300401100" +
                                                "34d83b0500" +
                                                "3579b72001" +
                                                "36700e0900" +
                                                "37c3010000" +
                                                "39ce030000" +
                                                "3a4c000000" +
                                                "3b4e000000" +
                                                "3c8a010100" +
                                                "3db8ab0000" +
                                                "404c010000" +
                                                "be1e4b00c0" +
                                                "45002d0000" +
                                                "fa3b010000" +
                                                "d7" +
                                                "5d"));

        assertNotNull(result);
        Position p = (Position) ((java.util.List<?>) result).get(0);

        assertEquals("2026-03-05 09:53:14.000", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS") {
                {
                        setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                }
        }.format(p.getFixTime()));
        assertTrue(p.getValid());
        assertEquals(55.237743, p.getLatitude(), 0.00001);
        assertEquals(86.137871, p.getLongitude(), 0.00001);
        assertEquals(232.0, p.getCourse(), 0.1);
        assertEquals(200.0, p.getAltitude(), 0.1);
        assertEquals(41.0, p.getSpeed(), 0.1);

        assertEquals(12, ((Number) p.getAttributes().get(Position.KEY_SATELLITES)).intValue());
        assertEquals(0.62, ((Number) p.getAttributes().get(Position.KEY_HDOP)).doubleValue(), 0.01);

        assertEquals(27.741, ((Number) p.getAttributes().get(Position.KEY_POWER)).doubleValue(), 0.01);
        assertEquals(4.021, ((Number) p.getAttributes().get(Position.KEY_BATTERY)).doubleValue(), 0.01);

        assertEquals(1130496L, ((Number) p.getAttributes().get("canStatus")).longValue());
        assertEquals(974, ((Number) p.getAttributes().get(Position.KEY_RPM)).intValue());
        assertEquals(76.0, ((Number) p.getAttributes().get(Position.KEY_ENGINE_TEMP)).doubleValue(), 0.1);
        assertEquals(78.0, ((Number) p.getAttributes().get(Position.KEY_OBD_SPEED)).doubleValue(), 0.1);
        assertEquals(45.1, ((Number) p.getAttributes().get(Position.KEY_FUEL_LEVEL)).doubleValue(), 0.1);
        assertEquals(59352.0, ((Number) p.getAttributes().get(Position.KEY_FUEL_USED)).doubleValue(), 0.1);
        assertEquals(0, ((Number) p.getAttributes().get(Position.KEY_ENGINE_LOAD)).intValue());
        assertEquals(45, ((Number) p.getAttributes().get(Position.KEY_THROTTLE)).intValue());

        assertEquals(12348000000L, ((Number) p.getAttributes().get(Position.KEY_HOURS)).longValue());
        assertEquals(189213370L, ((Number) p.getAttributes().get(Position.KEY_ODOMETER)).longValue());

        assertEquals(65930L, ((Number) p.getAttributes().get("axleLoad1")).longValue());
        assertEquals(43960L, ((Number) p.getAttributes().get(Position.KEY_AXLE_WEIGHT)).longValue());
        assertEquals(332L, ((Number) p.getAttributes().get("axleLoad5")).longValue());

        assertEquals(78, ((Number) p.getAttributes().get("rawFuelLevel1")).intValue());
        assertEquals(-10.0, ((Number) p.getAttributes().get("llsTemperture1")).doubleValue(), 0.1);

        CellTower cell = p.getNetwork().getCellTowers().iterator().next();
        assertEquals(250, (int) cell.getMobileCountryCode());
        assertEquals(30, (int) cell.getMobileNetworkCode());
        assertEquals(45148, (int) cell.getLocationAreaCode());
        assertEquals(13002, (long) cell.getCellId());
        assertEquals(-99, (int) cell.getSignalStrength());
                }
}