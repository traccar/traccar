package org.traccar;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.traccar.helper.DataConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Command;
import org.traccar.model.Position;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProtocolTest extends BaseTest {

    protected Position position(String time, boolean valid, double lat, double lon) throws ParseException {

        Position position = new Position();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        position.setTime(dateFormat.parse(time));
        position.setValid(valid);
        position.setLatitude(lat);
        position.setLongitude(lon);

        return position;
    }

    private String concatenateStrings(String... strings) {
        StringBuilder builder = new StringBuilder();
        for (String s : strings) {
            builder.append(s);
        }
        return builder.toString();
    }

    protected ByteBuf concatenateBuffers(ByteBuf... buffers) {
        ByteBuf result = Unpooled.buffer();
        for (ByteBuf buf : buffers) {
            result.writeBytes(buf);
        }
        return result;
    }

    protected ByteBuf binary(String... data) {
        return Unpooled.wrappedBuffer(DataConverter.parseHex(concatenateStrings(data)));
    }

    protected String text(String... data) {
        return concatenateStrings(data);
    }

    protected ByteBuf buffer(String... data) {
        return Unpooled.copiedBuffer(concatenateStrings(data), StandardCharsets.ISO_8859_1);
    }

    protected DefaultFullHttpRequest request(String url) {
        return request(HttpMethod.GET, url);
    }

    protected DefaultFullHttpRequest request(HttpMethod method, String url) {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, url);
    }

    protected DefaultFullHttpRequest request(HttpMethod method, String url, ByteBuf data) {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, url, data);
    }

    protected DefaultFullHttpRequest request(HttpMethod method, String url, HttpHeaders headers) {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, url, Unpooled.buffer(), headers, new DefaultHttpHeaders());
    }

    protected void verifyNotNull(BaseProtocolDecoder decoder, Object object) throws Exception {
        assertNotNull(decoder.decode(null, null, object));
    }

    protected void verifyNull(Object object) {
        assertNull(object);
    }

    protected void verifyNull(BaseProtocolDecoder decoder, Object object) throws Exception {
        assertNull(decoder.decode(null, null, object));
    }

    protected void verifyAttribute(BaseProtocolDecoder decoder, Object object, String key, Object expected) throws Exception {
        Object decodedObject = decoder.decode(null, null, object);
        Position position;
        if (decodedObject instanceof Collection) {
            position = (Position) ((Collection) decodedObject).iterator().next();
        } else {
            position = (Position) decodedObject;
        }
        switch (key) {
            case "speed":
                assertEquals(expected, position.getSpeed());
                break;
            case "course":
                assertEquals(expected, position.getCourse());
                break;
            default:
                assertEquals(expected, position.getAttributes().get(key));
                break;
        }
    }

    protected void verifyAttributes(BaseProtocolDecoder decoder, Object object) throws Exception {
        verifyDecodedPosition(decoder.decode(null, null, object), false, true, null);
    }

    protected void verifyPosition(BaseProtocolDecoder decoder, Object object) throws Exception {
        verifyDecodedPosition(decoder.decode(null, null, object), true, false, null);
    }

    protected void verifyPosition(BaseProtocolDecoder decoder, Object object, Position position) throws Exception {
        verifyDecodedPosition(decoder.decode(null, null, object), true, false, position);
    }

    protected void verifyPositions(BaseProtocolDecoder decoder, Object object) throws Exception {
        verifyDecodedList(decoder.decode(null, null, object), true, null);
    }

    protected void verifyPositions(BaseProtocolDecoder decoder, boolean checkLocation, Object object) throws Exception {
        verifyDecodedList(decoder.decode(null, null, object), checkLocation, null);
    }

    protected void verifyPositions(BaseProtocolDecoder decoder, Object object, Position position) throws Exception {
        verifyDecodedList(decoder.decode(null, null, object), true, position);
    }

    private void verifyDecodedList(Object decodedObject, boolean checkLocation, Position expected) {

        assertNotNull("list is null", decodedObject);
        assertTrue("not a list", decodedObject instanceof List);
        assertFalse("list is empty", ((List) decodedObject).isEmpty());

        for (Object item : (List) decodedObject) {
            verifyDecodedPosition(item, checkLocation, false, expected);
        }

    }

    private void verifyDecodedPosition(Object decodedObject, boolean checkLocation, boolean checkAttributes, Position expected) {

        assertNotNull("position is null", decodedObject);
        assertTrue("not a position", decodedObject instanceof Position);

        Position position = (Position) decodedObject;

        if (checkLocation) {

            if (expected != null) {

                if (expected.getFixTime() != null) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    assertEquals("time", dateFormat.format(expected.getFixTime()), dateFormat.format(position.getFixTime()));
                }
                assertEquals("valid", expected.getValid(), position.getValid());
                assertEquals("latitude", expected.getLatitude(), position.getLatitude(), 0.00001);
                assertEquals("longitude", expected.getLongitude(), position.getLongitude(), 0.00001);

            } else {

                assertNotNull(position.getServerTime());
                assertNotNull(position.getFixTime());
                assertTrue("year > 1999", position.getFixTime().after(new Date(915148800000L)));
                assertTrue("time < +25 hours",
                        position.getFixTime().getTime() < System.currentTimeMillis() + 25 * 3600000);

                assertTrue("latitude >= -90", position.getLatitude() >= -90);
                assertTrue("latitude <= 90", position.getLatitude() <= 90);

                assertTrue("longitude >= -180", position.getLongitude() >= -180);
                assertTrue("longitude <= 180", position.getLongitude() <= 180);

            }

            assertTrue("altitude >= -12262", position.getAltitude() >= -12262);
            assertTrue("altitude <= 18000", position.getAltitude() <= 18000);

            assertTrue("speed >= 0", position.getSpeed() >= 0);
            assertTrue("speed <= 869", position.getSpeed() <= 869);

            assertTrue("course >= 0", position.getCourse() >= 0);
            assertTrue("course <= 360", position.getCourse() <= 360);

            assertNotNull("protocol is null", position.getProtocol());

            assertTrue("deviceId > 0", position.getDeviceId() > 0);

        }

        Map<String, Object> attributes = position.getAttributes();

        if (checkAttributes) {
            assertFalse("no attributes", attributes.isEmpty());
        }

        if (attributes.containsKey(Position.KEY_INDEX)) {
            assertTrue(attributes.get(Position.KEY_INDEX) instanceof Number);
        }

        if (attributes.containsKey(Position.KEY_HDOP)) {
            assertTrue(attributes.get(Position.KEY_HDOP) instanceof Number);
        }

        if (attributes.containsKey(Position.KEY_VDOP)) {
            assertTrue(attributes.get(Position.KEY_VDOP) instanceof Number);
        }

        if (attributes.containsKey(Position.KEY_PDOP)) {
            assertTrue(attributes.get(Position.KEY_PDOP) instanceof Number);
        }

        if (attributes.containsKey(Position.KEY_SATELLITES)) {
            assertTrue(attributes.get(Position.KEY_SATELLITES) instanceof Number);
        }

        if (attributes.containsKey(Position.KEY_SATELLITES_VISIBLE)) {
            assertTrue(attributes.get(Position.KEY_SATELLITES_VISIBLE) instanceof Number);
        }

        if (attributes.containsKey(Position.KEY_RSSI)) {
            assertTrue(attributes.get(Position.KEY_RSSI) instanceof Number);
        }

        if (attributes.containsKey(Position.KEY_ODOMETER)) {
            assertTrue(attributes.get(Position.KEY_ODOMETER) instanceof Number);
        }

        if (attributes.containsKey(Position.KEY_RPM)) {
            assertTrue(attributes.get(Position.KEY_RPM) instanceof Number);
        }

        if (attributes.containsKey(Position.KEY_FUEL_LEVEL)) {
            assertTrue(attributes.get(Position.KEY_FUEL_LEVEL) instanceof Number);
        }

        if (attributes.containsKey(Position.KEY_POWER)) {
            assertTrue(attributes.get(Position.KEY_POWER) instanceof Number);
        }

        if (attributes.containsKey(Position.KEY_BATTERY)) {
            assertTrue(attributes.get(Position.KEY_BATTERY) instanceof Number);
        }

        if (attributes.containsKey(Position.KEY_BATTERY_LEVEL)) {
            int batteryLevel = ((Number) attributes.get(Position.KEY_BATTERY_LEVEL)).intValue();
            assertTrue(batteryLevel <= 100 && batteryLevel >= 0);
        }

        if (attributes.containsKey(Position.KEY_CHARGE)) {
            assertTrue(attributes.get(Position.KEY_CHARGE) instanceof Boolean);
        }

        if (attributes.containsKey(Position.KEY_IGNITION)) {
            assertTrue(attributes.get(Position.KEY_IGNITION) instanceof Boolean);
        }

        if (attributes.containsKey(Position.KEY_MOTION)) {
            assertTrue(attributes.get(Position.KEY_MOTION) instanceof Boolean);
        }

        if (attributes.containsKey(Position.KEY_ARCHIVE)) {
            assertTrue(attributes.get(Position.KEY_ARCHIVE) instanceof Boolean);
        }

        if (attributes.containsKey(Position.KEY_DRIVER_UNIQUE_ID)) {
            assertTrue(attributes.get(Position.KEY_DRIVER_UNIQUE_ID) instanceof String);
        }

        if (attributes.containsKey(Position.KEY_STEPS)) {
            assertTrue(attributes.get(Position.KEY_STEPS) instanceof Number);
        }

        if (attributes.containsKey(Position.KEY_ROAMING)) {
            assertTrue(attributes.get(Position.KEY_ROAMING) instanceof Boolean);
        }

        if (attributes.containsKey(Position.KEY_HOURS)) {
            assertTrue(attributes.get(Position.KEY_HOURS) instanceof Number);
        }

        if (position.getNetwork() != null && position.getNetwork().getCellTowers() != null) {
            for (CellTower cellTower : position.getNetwork().getCellTowers()) {
                checkInteger(cellTower.getMobileCountryCode(), 0, 999);
                checkInteger(cellTower.getMobileNetworkCode(), 0, 999);
                checkInteger(cellTower.getLocationAreaCode(), 1, 65535);
                checkInteger(cellTower.getCellId(), 0, 268435455);
            }
        }

    }

    private void checkInteger(Object value, int min, int max) {
        assertNotNull("value is null", value);
        assertTrue("not int or long", value instanceof Integer || value instanceof Long);
        long number = ((Number) value).longValue();
        assertTrue("value too low", number >= min);
        assertTrue("value too high", number <= max);
    }

    protected void verifyCommand(
            BaseProtocolEncoder encoder, Command command, ByteBuf expected) {
        verifyFrame(expected, encoder.encodeCommand(command));
    }

    protected void verifyFrame(ByteBuf expected, Object object) {
        assertNotNull("buffer is null", object);
        assertTrue("not a buffer", object instanceof ByteBuf);
        assertEquals(ByteBufUtil.hexDump(expected), ByteBufUtil.hexDump((ByteBuf) object));
    }

}
