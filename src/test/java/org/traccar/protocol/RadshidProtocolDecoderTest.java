package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.traccar.NetworkMessage;
import org.traccar.ProtocolTest;
import org.traccar.helper.DataConverter;
import org.traccar.model.Position;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RadshidProtocolDecoderTest extends ProtocolTest {

    private static final String SAMPLE_TWO_EVENTS =
            "600000000114fcfb160201e0000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a010001f3000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a0100657a";

    private static final String SAMPLE_EXTRA =
            "5200000001001e9bfd01e00100000000000000659d4c795151635b04df000000130001e275e00015896cf81e04d54a052401300e0f1e020b067f000000a33d0000000000000000000000001c6b000b29a5a514a5cdd9";

    private static final int TIMESTAMP = 1704807545;

    private static final String FIX_TIME = "2024-01-09T13:39:05Z";

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new RadshidProtocolDecoder(null));

        verifyDecode(decoder, binary(SAMPLE_TWO_EVENTS),
                position(),
                position());

        verifyDecode(decoder, binary(SAMPLE_EXTRA),
                position());

    }

    @Test
    public void testCrc() throws Exception {

        var decoder = inject(new RadshidProtocolDecoder(null));
        ByteBuf sample = binary(SAMPLE_TWO_EVENTS);
        int payloadLength = sample.getIntLE(0);
        int calculated = RadshidProtocolDecoder.crc16(sample, 4, payloadLength - 2);
        assertEquals(sample.getUnsignedShortLE(4 + payloadLength - 2), calculated);

        ByteBuf payloadModified = binary(SAMPLE_TWO_EVENTS);
        payloadModified.setByte(11, payloadModified.getByte(11) ^ 0x01);
        verifyDecode(decoder, payloadModified);

        ByteBuf crcModified = binary(SAMPLE_TWO_EVENTS);
        crcModified.setByte(crcModified.writerIndex() - 1, crcModified.getByte(crcModified.writerIndex() - 1) ^ 0x01);
        verifyDecode(decoder, crcModified);

        var channel = channel();
        assertNull(decoder.decode(channel, new InetSocketAddress(0), binary(SAMPLE_TWO_EVENTS).setByte(11, 0x00)));
        assertNull(channel.readOutbound());

        var shortChannel = channel();
        assertNull(decoder.decode(shortChannel, new InetSocketAddress(0), binary("6000000001")));
        assertNull(shortChannel.readOutbound());

    }

    @Test
    public void testAcknowledgement() throws Exception {

        var decoder = inject(new RadshidProtocolDecoder(null));
        var channel = channel();
        Object decoded = decoder.decode(channel, new InetSocketAddress(0), binary(SAMPLE_TWO_EVENTS));

        assertEquals(2, ((List<?>) decoded).size());
        NetworkMessage message = channel.readOutbound();
        ByteBuf response = (ByteBuf) message.getMessage();
        // Length 0x60 echoed big-endian. GSMPackGenerator.c:192-198, GSMTask.c bigpack->len.
        assertEquals("0700000082010000006001", ByteBufUtil.hexDump(response));

    }

    @Test
    public void testDistanceAndGeofence() throws Exception {

        var decoder = inject(new RadshidProtocolDecoder(null));

        verifyDecode(decoder, frame(event(0xE0, 0, 0x00, 0x1234, 0x80010203L)),
                position()
                        .attribute("geofenceId", 0x1234)
                        .attribute(Position.KEY_DISTANCE, 0x010203L)
                        .attribute(Position.KEY_RPM, null)
                        .attribute(Position.KEY_ODOMETER, null));

        verifyDecode(decoder, frame(event(0xE0, 0, 0x00, 0, 0x80000000L)),
                position()
                        .attribute("geofenceId", 0)
                        .attribute(Position.KEY_DISTANCE, 0L)
                        .attribute(Position.KEY_ODOMETER, null));

    }

    @Test
    public void testEvents() throws Exception {

        var decoder = inject(new RadshidProtocolDecoder(null));
        Map<Integer, String> alarms = new LinkedHashMap<>();
        alarms.put(0xD6, Position.ALARM_JAMMING);
        alarms.put(0xD7, Position.ALARM_JAMMING);
        alarms.put(0xD8, null);
        alarms.put(0xD9, null);
        alarms.put(0xDA, null);
        alarms.put(0xDB, Position.ALARM_REMOVING);
        alarms.put(0xDC, Position.ALARM_GEOFENCE_ENTER);
        alarms.put(0xDD, Position.ALARM_GEOFENCE_EXIT);
        alarms.put(0xDE, null);
        alarms.put(0xDF, null);
        alarms.put(0xE0, null);
        alarms.put(0xE1, null);
        alarms.put(0xE2, Position.ALARM_POWER_CUT);
        alarms.put(0xE3, Position.ALARM_POWER_RESTORED);
        alarms.put(0xE4, null);
        alarms.put(0xE5, Position.ALARM_ACCELERATION);
        alarms.put(0xE6, Position.ALARM_CORNERING);
        alarms.put(0xE7, null);
        alarms.put(0xEB, Position.ALARM_SOS);
        alarms.put(0xEC, null);
        alarms.put(0xED, null);
        alarms.put(0xEE, Position.ALARM_OVERSPEED);
        alarms.put(0xEF, null);
        alarms.put(0xF0, Position.ALARM_LOW_BATTERY);
        alarms.put(0xF1, null);
        alarms.put(0xF2, null);
        alarms.put(0xF5, null);
        alarms.put(0xF7, null);
        alarms.put(0xF9, null);
        alarms.put(0xFC, null);
        alarms.put(0xFE, null);

        for (var entry : alarms.entrySet()) {
            verifyDecode(decoder, frame(event(entry.getKey(), 0, 0x00, 0, 0)),
                    position()
                            .attribute(Position.KEY_EVENT, entry.getKey())
                            .attribute(Position.KEY_ALARM, entry.getValue()));
        }

    }

    @Test
    public void testIoAndGpsStatus() throws Exception {

        var decoder = inject(new RadshidProtocolDecoder(null));

        verifyDecode(decoder, frame(event(0xE0, 0x80, 0x00, 0, 0)),
                located(true)
                        .attribute("externalPower", true)
                        .attribute(Position.KEY_IGNITION, false)
                        .attribute("sos", false)
                        .attribute(Position.KEY_CHARGE, null)
                        .attribute(Position.KEY_GPS, 0x00)
                        .attribute(Position.KEY_APPROXIMATE, null));

        verifyDecode(decoder, frame(event(0xE0, 0x40, 0x02, 0, 0)),
                located(true)
                        .attribute("externalPower", false)
                        .attribute(Position.KEY_IGNITION, true)
                        .attribute("sos", false)
                        .attribute(Position.KEY_GPS, 0x02)
                        .attribute(Position.KEY_APPROXIMATE, null));

        verifyDecode(decoder, frame(event(0xE0, 0x20, 0x01, 0, 0)),
                located(false)
                        .attribute("externalPower", false)
                        .attribute(Position.KEY_IGNITION, false)
                        .attribute("sos", true)
                        .attribute(Position.KEY_GPS, 0x01)
                        .attribute(Position.KEY_APPROXIMATE, null));

        verifyDecode(decoder, frame(event(0xE0, 0xE0, 0x03, 0, 0)),
                located(true)
                        .attribute("externalPower", true)
                        .attribute(Position.KEY_IGNITION, true)
                        .attribute("sos", true)
                        .attribute(Position.KEY_GPS, 0x03)
                        .attribute(Position.KEY_APPROXIMATE, true));

    }

    @Test
    public void testExtraBlocks() throws Exception {

        var decoder = inject(new RadshidProtocolDecoder(null));

        verifyDecode(decoder, frame(
                        event(0xE2, 0, 0x00, 0, 0, debugExtra(7, 36)),
                        event(0xF1, 0, 0x00, 0, 0, 36.0)),
                extraExpectation(false, false, false)
                        .attribute(Position.KEY_EVENT, 0xE2)
                        .attribute(Position.KEY_ALARM, Position.ALARM_POWER_CUT)
                        .location(FIX_TIME, true, 35.0, 51.0),
                position()
                        .attribute(Position.KEY_EVENT, 0xF1)
                        .attribute(Position.KEY_ALARM, null)
                        .location(FIX_TIME, true, 36.0, 51.0));

        verifyDecode(decoder, frame(event(0xE0, 0, 0x00, 0, 0, debugExtra(8, 37))),
                extraExpectation(true, false, false));

        verifyDecode(decoder, frame(event(0xE0, 0, 0x00, 0, 0, debugExtra(10, 45))),
                extraExpectation(true, true, false));

        verifyDecode(decoder, frame(event(0xE0, 0, 0x00, 0, 0, debugExtra(13, 73))),
                extraExpectation(true, true, true));

        verifyDecode(decoder, frame(event(0xE0, 0, 0x00, 0, 0, debugExtra(11, 73))),
                extraExpectation(true, true, true));

        verifyDecode(decoder, frame(
                        event(0xE0, 0, 0x00, 0, 0, debugExtra(13, 27)),
                        event(0xEE, 0, 0x00, 0, 0, 36.0)),
                position()
                        .attribute(Position.KEY_VERSION_FW, 131)
                        .attribute("resetSource", 4)
                        .attribute(Position.PREFIX_TEMP + 1, null)
                        .attribute(Position.KEY_HUMIDITY, null)
                        .attribute(Position.KEY_EVENT, 0xE0)
                        .location(FIX_TIME, true, 35.0, 51.0),
                position()
                        .attribute(Position.KEY_EVENT, 0xEE)
                        .attribute(Position.KEY_ALARM, Position.ALARM_OVERSPEED)
                        .location(FIX_TIME, true, 36.0, 51.0));

        verifyDecode(decoder, frame(
                        event(0xE0, 0, 0x00, 0, 0, new byte[] {0x09, 0x11, 0x22, 0x33}),
                        event(0xF1, 0, 0x00, 0, 0, 36.0)),
                position()
                        .attribute(Position.KEY_VERSION_FW, null)
                        .attribute(Position.PREFIX_TEMP + 1, null)
                        .attribute(Position.KEY_EVENT, 0xE0)
                        .location(FIX_TIME, true, 35.0, 51.0),
                position()
                        .attribute(Position.KEY_EVENT, 0xF1)
                        .location(FIX_TIME, true, 36.0, 51.0));

        verifyDecode(decoder, frame(
                        event(0xE0, 0, 0x00, 0, 0, debugExtra(17, 73)),
                        event(0xF1, 0, 0x00, 0, 0, 36.0)),
                position()
                        .attribute(Position.KEY_VERSION_FW, null)
                        .attribute(Position.PREFIX_TEMP + 1, null)
                        .attribute(Position.KEY_EVENT, 0xE0),
                position()
                        .attribute(Position.KEY_EVENT, 0xF1)
                        .location(FIX_TIME, true, 36.0, 51.0));

    }

    private PositionExpectation located(boolean valid) {
        return position().location(FIX_TIME, valid, 35.0, 51.0);
    }

    private PositionExpectation extraExpectation(boolean staticLocation, boolean iccid, boolean cell) {
        PositionExpectation expectation = position()
                .location(FIX_TIME, true, 35.0, 51.0)
                .attribute(Position.KEY_VERSION_FW, 131)
                .attribute(Position.KEY_INDEX, 0x01020304L)
                .attribute("uptime", 3600L)
                .attribute("resetCount", 7)
                .attribute("storedPoints", 42)
                .attribute("simCharge", -15)
                .attribute("pauseTime", 90)
                .attribute("offTime", 30)
                .attribute(Position.KEY_RSSI, 18)
                .attribute("deviceStatus", 72)
                .attribute(Position.KEY_ARCHIVE, true)
                .attribute(Position.KEY_POWER, 12.6)
                .attribute(Position.KEY_BATTERY_LEVEL, 81)
                .attribute("savedPicture", true)
                .attribute("resetSource", 4)
                .attribute(Position.PREFIX_TEMP + 1, 23.5)
                .attribute(Position.KEY_HUMIDITY, 55.25)
                .attribute("crc8", 0xAB);
        if (staticLocation) {
            expectation.attribute("staticLocation", true);
        } else {
            expectation.attribute("staticLocation", null);
        }
        if (iccid) {
            expectation.attribute(Position.KEY_ICCID, "899811000000000001");
        } else {
            expectation.attribute(Position.KEY_ICCID, null);
        }
        if (cell) {
            expectation
                    .attribute("bcch", 100)
                    .attribute("bsic", 12)
                    .network(network().cell(cell()
                            .mcc(432)
                            .mnc(11)
                            .lac(1234)
                            .cid(56789)
                            .signal(-75)));
        }
        return expectation;
    }

    private ByteBuf frame(EventData... events) {
        ByteBuf body = Unpooled.buffer();
        body.writeByte(0x01);
        body.writeInt(0x14FCFB16);
        body.writeByte(events.length);
        for (EventData event : events) {
            writeEvent(body, event);
        }
        body.writeShortLE(RadshidProtocolDecoder.crc16(body, 0, body.readableBytes()));
        ByteBuf packet = Unpooled.buffer();
        packet.writeIntLE(body.readableBytes());
        packet.writeBytes(body);
        body.release();
        byte[] data = new byte[packet.readableBytes()];
        packet.readBytes(data);
        packet.release();
        return binary(DataConverter.printHex(data));
    }

    private void writeEvent(ByteBuf buf, EventData event) {
        buf.writeByte(0x01);
        buf.writeByte(event.eventCode);
        buf.writeZero(5);
        buf.writeShort(0);
        buf.writeInt(TIMESTAMP);
        buf.writeByte(10);
        buf.writeByte(0);
        buf.writeByte(0);
        buf.writeByte(0);
        buf.writeShort(event.geofenceId);
        buf.writeInt((int) event.distance);
        buf.writeInt(0x11223344);
        buf.writeByte(event.ioStatus);
        buf.writeByte(event.gpsStatus);
        buf.writeInt((int) Math.round(event.latitude * 10000000));
        buf.writeInt((int) Math.round(51.0 * 10000000));
        buf.writeShort(100);
        buf.writeShort(90);
        buf.writeByte(8);
        buf.writeByte(10);
        byte[] extra = event.extra == null ? new byte[0] : event.extra;
        buf.writeByte(extra.length);
        buf.writeBytes(extra);
    }

    private EventData event(int eventCode, int ioStatus, int gpsStatus, int geofenceId, long distance) {
        return event(eventCode, ioStatus, gpsStatus, geofenceId, distance, null);
    }

    private EventData event(
            int eventCode, int ioStatus, int gpsStatus, int geofenceId, long distance, byte[] extra) {
        return new EventData(eventCode, ioStatus, gpsStatus, geofenceId, distance, 35.0, extra);
    }

    private EventData event(
            int eventCode, int ioStatus, int gpsStatus, int geofenceId, long distance, double latitude) {
        return new EventData(eventCode, ioStatus, gpsStatus, geofenceId, distance, latitude, null);
    }

    private byte[] debugExtra(int version, int length) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(version);
        buf.writeShortLE(131);
        buf.writeIntLE(0x01020304);
        buf.writeIntLE(3600);
        buf.writeShortLE(7);
        buf.writeShortLE(42);
        buf.writeShortLE(-15);
        buf.writeShortLE(90);
        buf.writeShortLE(30);
        buf.writeByte(18);
        buf.writeByte(72);
        buf.writeByte(126);
        buf.writeByte(81);
        buf.writeByte(1);
        buf.writeByte(4);
        buf.writeFloatLE(23.5f);
        buf.writeFloatLE(55.25f);
        buf.writeByte(0xAB);
        buf.writeByte(1);
        buf.writeLongLE(899811000000000001L);
        buf.writeIntLE(432);
        buf.writeIntLE(11);
        buf.writeIntLE(1234);
        buf.writeIntLE(56789);
        buf.writeIntLE(100);
        buf.writeIntLE(12);
        buf.writeIntLE(-75);
        byte[] data = new byte[length];
        buf.readBytes(data);
        buf.release();
        return data;
    }

    private record EventData(
            int eventCode, int ioStatus, int gpsStatus, int geofenceId, long distance, double latitude, byte[] extra) {
    }

}
