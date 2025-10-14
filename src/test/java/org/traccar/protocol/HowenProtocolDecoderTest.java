package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HowenProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new HowenProtocolDecoder(null));

        decoder.decode(null, null, binary(
                "48010110c80000007b226170223a22222c226174223a2231222c22646e223a223238303831313032222c226474223a22307834303030222c22647475223a22323031382d30392d31322032303a31383a3134222c2267756964223a2236423842343536372d32334336333237422d41393938334336342d3733343833333636222c226d62223a223238303831313032222c227373223a2236423842343536372d32334336333237422d41393938334336342d3733343833333636222c22766572223a22563138303832394230227d0a00"));

        Position status = (Position) decoder.decode(null, null, binary(
                "4801411083000000217374617475732d32383038313130322d303030303031453936424446423031300012090e0b0320af03000112090e0b031a000800001815090071d88f080016472905000700000400000002000000810000001f00000103010f000000000000010001eaed0000000000000f00000000000000000000003f0000000000000000000000"));

        assertNotNull(status);
        assertNotNull(status.getDeviceTime());
        assertNotNull(status.getFixTime());
        assertEquals(Instant.parse("2018-09-14T04:03:26Z"), status.getDeviceTime().toInstant());
        assertEquals(Instant.parse("2018-09-14T04:03:26Z"), status.getFixTime().toInstant());
        assertEquals(22.563745, status.getLatitude(), 0.00001);
        assertEquals(113.935187, status.getLongitude(), 0.00001);
        assertEquals(0.9, status.getAccuracy(), 0.01);
        assertEquals(54.0, status.getAltitude(), 0.1);
        assertEquals(8, status.getInteger(Position.KEY_SATELLITES));
        assertTrue(status.getBoolean(Position.KEY_IGNITION));
        assertTrue(status.getBoolean(Position.KEY_DOOR));
        assertEquals("status-28081102-000001E96BDFB010", status.getString("session"));
        assertEquals(0x07, status.getInteger("howenGSensorMask"));
        assertEquals(0, status.getInteger("howenAccelX"));
        assertEquals(4, status.getInteger("howenAccelY"));
        assertEquals(0, status.getInteger("howenAccelZ"));
        assertEquals(2, status.getInteger("howenTilt"));
        assertEquals(0, status.getInteger("howenImpact"));
        assertEquals(0x1f, status.getInteger("howenModuleMask"));
        assertEquals(0, status.getInteger("howenModuleMobile"));
        assertEquals(1, status.getInteger("howenModuleLocation"));
        assertEquals(3, status.getInteger("howenModuleWifi"));
        assertEquals(1, status.getInteger("howenModuleGSensor"));
        assertEquals(15, status.getInteger("howenModuleRecording"));
        assertEquals(0, status.getInteger(Position.KEY_RSSI));
        assertEquals(0, status.getInteger("howenSignal"));
        assertEquals(0, status.getInteger("howenNetworkType"));
        assertEquals(1, status.getInteger("howenStorageMask"));
        assertEquals(0, status.getInteger("howenStorageDisk"));
        assertEquals(1, status.getInteger("howenStorageStatus"));
        assertEquals(60906L, status.getLong("howenStorageSize"));
        assertEquals(0L, status.getLong("howenStorageAvailable"));
        assertEquals(0x0fL, status.getLong("howenAlarmMask"));
        assertEquals(0, status.getInteger("howenAlarmVideoLoss"));
        assertEquals(0, status.getInteger("howenAlarmMotion"));
        assertEquals(0, status.getInteger("howenAlarmCover"));
        assertEquals(0, status.getInteger("howenAlarmInput"));
        assertEquals(0x3f, status.getInteger("howenTempMask"));
        assertEquals(0, status.getInteger("howenTempIn"));
        assertEquals(0, status.getInteger("howenTempOut"));
        assertEquals(0, status.getInteger("howenTempEngine"));
        assertEquals(0, status.getInteger("howenTempDevice"));
        assertEquals(0, status.getInteger("howenHumidityIn"));
        assertEquals(0, status.getInteger("howenHumidityOut"));

        Position alarm = (Position) decoder.decode(null, null, binary(
                "48015110bc00000020616c61726d2d32383038313130322d3030303030314539364244464230313000640000007b22646574223a7b226368223a2231227d2c22647475223a22323031382d30392d31342031343a33313a3037222c226563223a2232222c226574223a22222c227061223a22222c227374223a22323031382d30392d31342031343a33313a3037227d0a0012090e0e1f072d00000112090e0e1f07000b00008214080071588f0800165a280500810000001f00000103010f000000000000"));

        assertNotNull(alarm);
        assertNotNull(alarm.getDeviceTime());
        assertNotNull(alarm.getFixTime());
        assertEquals(Instant.parse("2018-09-14T07:31:07Z"), alarm.getDeviceTime().toInstant());
        assertEquals(Instant.parse("2018-09-14T07:31:07Z"), alarm.getFixTime().toInstant());
        assertEquals(22.56335, alarm.getLatitude(), 0.00001);
        assertEquals(113.934973, alarm.getLongitude(), 0.00001);
        assertEquals(52.5, alarm.getAltitude(), 0.1);
        assertEquals(2, alarm.getInteger(Position.KEY_EVENT));
        assertEquals("{\"ch\":\"1\"}", alarm.getString("alarmDetail"));
        assertEquals("alarm-28081102-000001E96BDFB010", alarm.getString("session"));
        assertEquals(0x1f, alarm.getInteger("howenModuleMask"));
        assertEquals(0L, alarm.getLong("howenAlarmMask"));

        decoder.decode(null, null, binary(
                "48010110d10100007b22616c67223a22312e302e302e3530222c226170223a22222c226174223a2235222c226469616c"
                        + "223a224543323541555847415230384131314d3147222c22646e223a22373038303439333832373934222c226474223a"
                        + "22307834303030222c22647475223a22323032352d31302d30372031353a30353a3334222c22657874223a2254323530"
                        + "3932353031222c226677223a224d4534302d30325638222c22676d74223a222b30373a3030222c2267756964223a2236"
                        + "423842343536372d32334336333237422d41393938334336342d3733343833333636222c226865223a2233222c226875"
                        + "62223a2248574855423032432d4d43552d555047524144452d473235303531333031222c226877223a223831222c2269"
                        + "63636964223a223839363630333235313030313636303235313246222c22696d6569223a223836323730383034393338"
                        + "32373934222c226970223a2231302e37332e3139362e323137222c226d62223a22373038303439333832373934222c22"
                        + "6d6375223a22473234303630353031222c227373223a2236423842343536372d32334336333237422d41393938334336"
                        + "342d3733343833333636222c22756d223a2230222c22766572223a22543235303932353031227d0a00"));

        Position alarmWithoutLocation = (Position) decoder.decode(null, null, binary(
                "48015110250100002436423842343536372d32334336333237422d41393938334336342d373334383333363600fc000000"
                        + "7b22646574223a7b22617667223a2230222c22637572223a2230222c226474223a2237222c226d6178223a2230222c"
                        + "226d696e223a2230222c22707265223a2230222c227474223a2237222c227674223a223330227d2c2264726964223a"
                        + "22222c2264726e616d65223a22222c22647475223a22323032352d30392d32362031303a30363a3135222c22656322"
                        + "3a223132222c226574223a22222c2266656e63656964223a22222c2273706473223a2230222c227374223a22323032"
                        + "352d30392d32362031303a30363a3135222c2275756964223a22313735383838313137353031303030303036303030"
                        + "30373038303439333832373934227d0a00"));

        assertNotNull(alarmWithoutLocation);
        assertNotNull(alarmWithoutLocation.getFixTime());
        assertEquals(Instant.parse("2025-09-25T20:06:15Z"), alarmWithoutLocation.getFixTime().toInstant());
        assertEquals(Instant.parse("2025-09-25T20:06:15Z"), alarmWithoutLocation.getDeviceTime().toInstant());
        assertEquals(alarmWithoutLocation.getDeviceTime(), alarmWithoutLocation.getFixTime());
        assertEquals(12, alarmWithoutLocation.getInteger(Position.KEY_EVENT));
    }

    @Test
    public void testAlarmJsonPayload() throws Exception {

        var decoder = inject(new HowenProtocolDecoder(null));

        decoder.decode(null, null, binary(
                "48010110c80000007b226170223a22222c226174223a2231222c22646e223a223238303831313032222c226474223a22307834303030222c22647475223a22323031382d30392d31322032303a31383a3134222c2267756964223a2236423842343536372d32334336333237422d41393938334336342d3733343833333636222c226d62223a223238303831313032222c227373223a2236423842343536372d32334336333237422d41393938334336342d3733343833333636222c22766572223a22563138303832394230227d0a00"));

        Position alarm = (Position) decoder.decode(null, null, binary(
                "480151102a0100002436423842343536372d32334336333237422d41393938334336342d373334383333363600"
                        + "010100007b22646574223a7b22617667223a223332222c2264726964223a22222c22647572223a2235393631222c22"
                        + "6d6178223a22313138222c226d696c65223a223533383337222c22736c6174223a22302e303030303030222c22736c"
                        + "6e67223a22302e303030303030227d2c2264726964223a22222c2264726e616d65223a22222c22647475223a223230"
                        + "32352d30382d31332030383a33333a3133222c226563223a22373638222c226574223a22323032352d30382d313320"
                        + "30383a33333a3133222c2266656e63656964223a22222c2273706473223a2230222c227374223a22323032352d3038"
                        + "2d31332030363a35333a3532222c2275756964223a22227d0a00"));

        assertNotNull(alarm);
        assertEquals(768, alarm.getInteger(Position.KEY_EVENT));
        assertEquals("{\"avg\":\"32\",\"drid\":\"\",\"dur\":\"5961\",\"max\":\"118\",\"mile\":\"53837\",\"slat\":\"0.000000\",\"slng\":\"0.000000\"}",
                alarm.getString("alarmDetail"));
        assertFalse(alarm.getValid());
        assertNotNull(alarm.getDeviceTime());
        assertEquals(0, alarm.getInteger("howenModuleMask"));
    }
}