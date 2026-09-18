package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import org.traccar.NetworkMessage;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class T800xAssetProtocolDecoderTest extends ProtocolTest {

    // Synthetic frames based on the supplied KnightX/CyberLabelX field definitions.
    // Lengths and CRC8 bytes were calculated independently of the decoder.
    private static final String GPS =
            "272762008c123408806168988888884cc12609181020300104800064c00000003cf000000000000000010101"
            + "020e010002c4034b07500aa0600d5c61880062003c630fa06404b4650544661a1ba00000255ba20000255a"
            + "03000200011100006a420000544100005242005a00a60900041baabbccddeeff102030405060c8203040506070d0304050607080d8";

    private Position decode(T800xProtocolDecoder decoder, EmbeddedChannel channel, String hex) throws Exception {
        ByteBuf data = binary(hex);
        try {
            return (Position) decoder.decode(channel, null, data);
        } finally {
            data.release();
        }
    }

    private void assertReply(EmbeddedChannel channel, String hex) {
        NetworkMessage message = channel.readOutbound();
        assertNotNull(message);
        ByteBuf data = (ByteBuf) message.getMessage();
        try {
            assertEquals(hex, ByteBufUtil.hexDump(data));
        } finally {
            data.release();
        }
        assertNull(channel.readOutbound(), "Only one acknowledgement is allowed");
    }

    @Test
    public void testVariablePosition() throws Exception {
        var decoder = inject(new T800xProtocolDecoder(null));
        var channel = new EmbeddedChannel();
        Position position = decode(decoder, channel, GPS);
        assertNotNull(position);
        assertTrue(position.getValid());
        assertFalse(position.getOutdated());
        assertEquals(Instant.parse("2026-09-18T10:20:30Z"), position.getFixTime().toInstant());
        assertEquals(52.5, position.getLatitude());
        assertEquals(13.25, position.getLongitude());
        assertEquals(58.5, position.getAltitude());
        assertEquals(90, position.getCourse());
        assertEquals(60 / 1.852, position.getSpeed(), 0.0001);
        assertEquals(80, position.getInteger(Position.KEY_BATTERY_LEVEL));
        assertEquals(3.420, position.getDouble(Position.KEY_BATTERY), 0.00001);
        assertEquals(-4.52, position.getDouble(Position.PREFIX_TEMP + 1), 0.00001);
        assertEquals(26.27, position.getDouble(Position.KEY_DEVICE_TEMP), 0.00001);
        assertEquals(9563, position.getLong(Position.KEY_ODOMETER));
        assertTrue(position.getBoolean(Position.KEY_ARCHIVE));
        assertEquals(3, position.getNetwork().getWifiAccessPoints().size());
        assertTrue(position.getNetwork().getWifiAccessPoints().stream()
                .anyMatch(ap -> ap.getSignalStrength() == -56));
        assertReply(channel, "272762000f12340880616898888888");
        channel.finishAndReleaseAll();
    }

    @Test
    public void testLteAlarmAndAcknowledgement() throws Exception {
        var decoder = inject(new T800xProtocolDecoder(null));
        var channel = new EmbeddedChannel();
        Position position = decode(decoder, channel,
                "272764003312340880616898888888264126091810203002020106028403000100031081ee0001001234560100010201030104");
        assertNotNull(position);
        assertFalse(position.getValid());
        assertTrue(position.getOutdated());
        assertEquals(Position.ALARM_LOW_BATTERY, position.getString(Position.KEY_ALARM));
        assertEquals("lte", position.getNetwork().getRadioType());
        var cell = position.getNetwork().getCellTowers().iterator().next();
        assertEquals(494, cell.getMobileCountryCode());
        assertEquals(1, cell.getMobileNetworkCode());
        assertEquals(0x123456L, cell.getCellId());
        assertEquals(256, cell.getLocationAreaCode());
        assertReply(channel, "27276400101234088061689888888806");

        assertNotNull(decode(decoder, channel, "272764001b12340880616898888888ed4126091810203002010100"));
        assertReply(channel, "27276400101234088061689888888800");
        channel.finishAndReleaseAll();
    }

    @Test
    public void testNoAcknowledgementRequested() throws Exception {
        var decoder = inject(new T800xProtocolDecoder(null));
        var channel = new EmbeddedChannel();
        String noAck = GPS.replace("4cc1", "d301");
        assertNotNull(decode(decoder, channel, noAck));
        assertNull(channel.readOutbound());
        channel.finishAndReleaseAll();
    }

    @Test
    public void testRejectedPacketsAreNotAcknowledged() throws Exception {
        var decoder = inject(new T800xProtocolDecoder(null));
        var channel = new EmbeddedChannel();
        for (String hex : new String[] {
                GPS.replace("4cc1", "4dc1"), // corrupted checksum
                GPS.substring(0, GPS.length() - 2), // truncated frame
                "272762001e123408806168988888886f4126091810203003000100010100", // short GPS block
                "2727620019123408806168988888887e412609181020300400", // unknown group
                GPS.replace("4cc1", "0461"), // encrypted format, unsupported
                "272762000f12340880616898888888", // missing payload
                "272762", // missing header
        }) {
            assertNull(decode(decoder, channel, hex), hex);
            assertNull(channel.readOutbound(), hex);
        }
        // A rejected packet must not poison the next valid packet.
        assertNotNull(decode(decoder, channel, GPS));
        assertReply(channel, "272762000f12340880616898888888");
        channel.finishAndReleaseAll();
    }

    @Test
    public void testUnknownFieldsAndUnavailableValues() throws Exception {
        var decoder = inject(new T800xProtocolDecoder(null));
        Position position = decode(decoder, null,
                "272762002612340880616898888888bd4126091810203002025f12074b030001123403010203");
        assertEquals(75, position.getInteger(Position.KEY_BATTERY_LEVEL));
        position = decode(decoder, null, "2727620021123408806168988888887b41260918102030020307ff60ffff64ffff");
        assertFalse(position.hasAttribute(Position.KEY_BATTERY_LEVEL));
        assertFalse(position.hasAttribute(Position.KEY_BATTERY));
        assertFalse(position.hasAttribute(Position.PREFIX_TEMP + 1));
        assertFalse(position.getValid());
    }

    @Test
    public void testInvalidGpsStatus() throws Exception {
        var decoder = inject(new T800xProtocolDecoder(null));
        Position position = decode(decoder, null,
                "27276200321234088061689888888816412609181020300201028003000100011100006a420000544100005242005a00a609");
        assertFalse(position.getValid());
        assertTrue(position.getOutdated());
    }

    @Test
    public void testSolarLock() throws Exception {
        var decoder = inject(new T800xProtocolDecoder(null));
        var channel = new EmbeddedChannel();
        Position position = decode(decoder, channel,
                "272717002c12340880616898888888260918102030c900006a4200005441000052420060005a210412345678");
        assertTrue(position.getValid());
        assertFalse(position.getOutdated());
        assertEquals(52.5, position.getLatitude());
        assertEquals(Instant.parse("2026-09-18T10:20:30Z"), position.getFixTime().toInstant());
        assertEquals(0x21, position.getInteger("lockType"));
        assertFalse(position.getBoolean(Position.KEY_LOCK));
        assertEquals("12345678", position.getString("lockId"));
        assertTrue(position.getBoolean(Position.KEY_ARCHIVE));
        assertReply(channel, "272717000f12340880616898888888");
        channel.finishAndReleaseAll();
    }

    @Test
    public void testSolarSubLock() throws Exception {
        var decoder = inject(new T800xProtocolDecoder(null));
        Position position = decode(decoder, null,
                "2727270047123408806168988888882609181020304900006a4200005441000052420060005a"
                + "4811208400062100210dcf14a11daabbccddeeff12345678020150000412345678");
        assertNotNull(position);
        assertEquals("aabbccddeeff", position.getString("lockId"));
        assertEquals("12345678", position.getString("lockDeviceId"));
        assertFalse(position.getBoolean(Position.KEY_LOCK));
        assertEquals(3.535, position.getDouble("lockBattery"), 0.00001);
        assertEquals(5.281, position.getDouble("lockSolarPanel"), 0.00001);
        assertEquals(80, position.getInteger("lockBatteryLevel"));
        assertEquals("12345678", position.getString("lockData0"));
    }

    @Test
    public void testWifiAlarm() throws Exception {
        var decoder = inject(new T800xProtocolDecoder(null));
        var channel = new EmbeddedChannel();
        Position position = decode(decoder, channel,
                "272725005212340880616898888888a008260918102030aabbccddeeff102030405060c8203040506070d0304050607080d8"
                + "800000800050a01539500000255a04b4003c0000012c1e006405000000000000");
        assertFalse(position.getValid());
        assertEquals(3, position.getNetwork().getWifiAccessPoints().size());
        assertEquals(50, position.getInteger(Position.KEY_BATTERY_LEVEL));
        assertEquals(-32, position.getInteger(Position.KEY_DEVICE_TEMP));
        assertEquals(3.9, position.getDouble(Position.KEY_BATTERY), 0.00001);
        assertEquals(Position.ALARM_TEMPERATURE, position.getString(Position.KEY_ALARM));
        assertReply(channel, "27272500101234088061689888888808");
        channel.finishAndReleaseAll();
    }

    @Test
    public void testTemperatureSamplesAndGeofences() throws Exception {
        var decoder = inject(new T800xProtocolDecoder(null));
        Position position = decode(decoder, null,
                "272726001e1234088061689888888826091810203001012c0af0fa24ffff");
        assertEquals(300, position.getInteger("samplingInterval"));
        assertEquals(28, position.getDouble(Position.PREFIX_TEMP + 1));
        assertEquals("[28.0, -15.0, null]", position.getString("temperatureSamples"));
        assertTrue(position.getOutdated());
        position = decode(decoder, null,
                "2727200026123408806168988888882609181020300101000d04000000640000544100005242");
        assertTrue(position.getBoolean("deviceGeofenceEnabled"));
        assertEquals("04000000640000544100005242", position.getString("deviceGeofence1"));
    }

    @Test
    public void testBleWithLocation() throws Exception {
        var decoder = inject(new T800xProtocolDecoder(null));
        var channel = new EmbeddedChannel();
        Position position = decode(decoder, channel,
                "272712003812340880616898888888260918102030014900006a4200005441000052420060005a"
                + "0004aabbccddeeff645009c41388123448");
        assertTrue(position.getValid());
        assertEquals(52.5, position.getLatitude());
        assertEquals("aabbccddeeff", position.getString("tag1Id"));
        assertEquals(25, position.getDouble("tag1Temp"));
        assertEquals(50, position.getDouble("tag1Humidity"));
        assertEquals(-56, position.getInteger("tag1Rssi"));
        assertReply(channel, "272712000f12340880616898888888");
        channel.finishAndReleaseAll();
    }

    @Test
    public void testSolarLegacyPositionAndLteFallback() throws Exception {
        var decoder = inject(new T800xProtocolDecoder(null));
        ByteBuf data = binary(
                "2727020049052e086528404072393849002008060310110000000068b7c8c286eaa441000000008000008100001617410700019ce782b0001e000002581e00000530d4801f00000000");
        try {
            data.setByte(45, 0xa0); // Signed-magnitude temperature: -32 degrees.
            data.setByte(70, 0x21); // Network unlock succeeded.
            Position position = (Position) decoder.decode(null, null, data.duplicate());
            assertTrue(position.getValid());
            assertEquals(-32, position.getInteger(Position.KEY_DEVICE_TEMP));
            assertEquals(0x21, position.getInteger("lockType"));
            assertFalse(position.getBoolean(Position.KEY_LOCK));

            data.setByte(15, 0x80); // Historical LBS position, no GNSS fix.
            ByteBuf cell = binary("81ee0001001234560100010201030104");
            try {
                data.setBytes(23, cell);
            } finally {
                cell.release();
            }
            position = (Position) decoder.decode(null, null, data.duplicate());
            assertFalse(position.getValid());
            assertTrue(position.getBoolean(Position.KEY_ARCHIVE));
            assertEquals(494, position.getNetwork().getCellTowers().iterator().next().getMobileCountryCode());
            assertEquals(0x123456L, position.getNetwork().getCellTowers().iterator().next().getCellId());
            assertEquals(-32, position.getInteger(Position.KEY_DEVICE_TEMP));
        } finally {
            data.release();
        }
    }

    @Test
    public void testRepeatedAndMixedBleRecords() throws Exception {
        var decoder = inject(new T800xProtocolDecoder(null));
        String header = "2727120000123408806168988888882609181020300149"
                + "00006a4200005441000052420060005a";
        String sensor = "aabbccddeeff645009c41388123448";
        for (String payload : new String[] {"0004" + sensor + sensor, "00000004" + sensor + "0004" + sensor}) {
            ByteBuf data = binary(header + payload);
            try {
                data.setShort(3, data.readableBytes());
                Position position = (Position) decoder.decode(null, null, data);
                assertNotNull(position);
                assertEquals(25, position.getDouble("tag2Temp"));
                assertEquals("aabbccddeeff", position.getString("tag2Id"));
            } finally {
                data.release();
            }
        }
    }
}
