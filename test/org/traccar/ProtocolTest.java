package org.traccar;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Assert;
import org.traccar.model.CellTower;
import org.traccar.model.Command;
import org.traccar.model.Position;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class ProtocolTest extends BaseTest {

    protected Position position(String time, boolean valid, double lat, double lon) throws ParseException {

        Position position = new Position();

        if (time != null) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            position.setTime(dateFormat.parse(time));
        }
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

    protected ChannelBuffer binary(String... data) {
        return binary(ByteOrder.BIG_ENDIAN, data);
    }

    protected ChannelBuffer binary(ByteOrder endianness, String... data) {
        return ChannelBuffers.wrappedBuffer(
                endianness, DatatypeConverter.parseHexBinary(concatenateStrings(data)));
    }

    protected String text(String... data) {
        return concatenateStrings(data);
    }

    protected ChannelBuffer buffer(String... data) {
        return ChannelBuffers.copiedBuffer(concatenateStrings(data), StandardCharsets.ISO_8859_1);
    }

    protected DefaultHttpRequest request(String url) {
        return request(HttpMethod.GET, url);
    }

    protected DefaultHttpRequest request(HttpMethod method, String url) {
        return new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, url);
    }

    protected DefaultHttpRequest request(HttpMethod method, String url, ChannelBuffer data) {
        DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, url);
        request.setContent(data);
        return request;
    }

    protected void verifyNotNull(BaseProtocolDecoder decoder, Object object) throws Exception {
        Assert.assertNotNull(decoder.decode(null, null, object));
    }

    protected void verifyNothing(BaseProtocolDecoder decoder, Object object) throws Exception {
        Assert.assertNull(decoder.decode(null, null, object));
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

        Assert.assertNotNull("list is null", decodedObject);
        Assert.assertTrue("not a list", decodedObject instanceof List);
        Assert.assertFalse("list if empty", ((List) decodedObject).isEmpty());

        for (Object item : (List) decodedObject) {
            verifyDecodedPosition(item, checkLocation, false, expected);
        }

    }

    private void verifyDecodedPosition(Object decodedObject, boolean checkLocation, boolean checkAttributes, Position expected) {

        Assert.assertNotNull("position is null", decodedObject);
        Assert.assertTrue("not a position", decodedObject instanceof Position);

        Position position = (Position) decodedObject;

        if (checkLocation) {

            if (expected != null) {

                if (expected.getFixTime() != null) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Assert.assertEquals("time", dateFormat.format(expected.getFixTime()), dateFormat.format(position.getFixTime()));
                }
                Assert.assertEquals("valid", expected.getValid(), position.getValid());
                Assert.assertEquals("latitude", expected.getLatitude(), position.getLatitude(), 0.00001);
                Assert.assertEquals("longitude", expected.getLongitude(), position.getLongitude(), 0.00001);

            } else {

                Assert.assertNotNull(position.getFixTime());
                Assert.assertTrue("year > 1999", position.getFixTime().after(new Date(915148800000L)));
                Assert.assertTrue("time < +25 hours",
                        position.getFixTime().getTime() < System.currentTimeMillis() + 25 * 3600000);

                Assert.assertTrue("latitude >= -90", position.getLatitude() >= -90);
                Assert.assertTrue("latitude <= 90", position.getLatitude() <= 90);

                Assert.assertTrue("longitude >= -180", position.getLongitude() >= -180);
                Assert.assertTrue("longitude <= 180", position.getLongitude() <= 180);

            }

            Assert.assertTrue("altitude >= -12262", position.getAltitude() >= -12262);
            Assert.assertTrue("altitude <= 18000", position.getAltitude() <= 18000);

            Assert.assertTrue("speed >= 0", position.getSpeed() >= 0);
            Assert.assertTrue("speed <= 869", position.getSpeed() <= 869);

            Assert.assertTrue("course >= 0", position.getCourse() >= 0);
            Assert.assertTrue("course <= 360", position.getCourse() <= 360);

            Assert.assertNotNull("protocol is null", position.getProtocol());

        }

        Map<String, Object> attributes = position.getAttributes();

        if (checkAttributes) {
            Assert.assertFalse("no attributes", attributes.isEmpty());
        }

        if (attributes.containsKey(Position.KEY_ODOMETER)) {
            Assert.assertTrue(attributes.get(Position.KEY_ODOMETER) instanceof Number);
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
        Assert.assertNotNull("value is null", value);
        Assert.assertTrue("not int or long", value instanceof Integer || value instanceof Long);
        long number = ((Number) value).longValue();
        Assert.assertTrue("value too low", number >= min);
        Assert.assertTrue("value too high", number <= max);
    }

    protected void verifyCommand(
            BaseProtocolEncoder encoder, Command command, ChannelBuffer expected) throws Exception {
        verifyDecodedCommand(encoder.encodeCommand(command), expected);
    }

    private void verifyDecodedCommand(Object decodedObject, ChannelBuffer expected) {

        Assert.assertNotNull("command is null", decodedObject);
        Assert.assertTrue("not a buffer", decodedObject instanceof ChannelBuffer);
        Assert.assertEquals(ChannelBuffers.hexDump(expected), ChannelBuffers.hexDump((ChannelBuffer) decodedObject));

    }

}
