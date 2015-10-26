package org.traccar;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Assert;
import org.traccar.database.IdentityManager;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.model.Device;
import org.traccar.model.Position;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ProtocolDecoderTest {

    static {
        Context.init(new IdentityManager() {

            private Device createDevice() {
                Device device = new Device();
                device.setId(1);
                device.setUniqueId("123456789012345");
                return device;
            }

            @Override
            public Device getDeviceById(long id) {
                return createDevice();
            }

            @Override
            public Device getDeviceByUniqueId(String imei) {
                return createDevice();
            }

        });
    }

    protected void verifyNothing(BaseProtocolDecoder decoder, Object object) throws Exception {
        Assert.assertNull(decoder.decode(null, null, object));
    }

    protected void verifyAttributes(BaseProtocolDecoder decoder, Object object) throws Exception {
        Object decodedObject = decoder.decode(null, null, object);
        Assert.assertNotNull(decodedObject);
        Assert.assertTrue(decodedObject instanceof Position);
        Position position = (Position) decodedObject;
        Assert.assertFalse(position.getAttributes().isEmpty());
    }

    protected void verifyPosition(BaseProtocolDecoder decoder, Object object) throws Exception {
        verifyDecodedPosition(decoder.decode(null, null, object));
    }

    protected void verifyPosition(BaseProtocolDecoder decoder, Object object, Position position) throws Exception {
        verifyDecodedPosition(decoder.decode(null, null, object), position);
    }

    protected void verifyPositions(BaseProtocolDecoder decoder, Object object) throws Exception {
        Object decodedObject = decoder.decode(null, null, object);
        Assert.assertNotNull(decodedObject);
        Assert.assertTrue(decodedObject instanceof List);
        for (Object item : (List) decodedObject) {
            verifyDecodedPosition(item);
        }
    }

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
        return ChannelBuffers.copiedBuffer(concatenateStrings(data), Charset.defaultCharset());
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

    private void verifyDecodedPosition(Object decodedObject, Position expected) {

        Assert.assertNotNull(decodedObject);
        Assert.assertTrue(decodedObject instanceof Position);

        Position position = (Position) decodedObject;

        if (expected.getFixTime() != null) {
            Assert.assertEquals("time", expected.getFixTime(), position.getFixTime());
        }
        Assert.assertEquals("valid", expected.getValid(), position.getValid());
        Assert.assertEquals("latitude", expected.getLatitude(), position.getLatitude(), 0.00001);
        Assert.assertEquals("longitude", expected.getLongitude(), position.getLongitude(), 0.00001);

    }

    private void verifyDecodedPosition(Object decodedObject) {

        Assert.assertNotNull("position is null", decodedObject);
        Assert.assertTrue("not a position", decodedObject instanceof Position);

        Position position = (Position) decodedObject;

        Assert.assertNotNull(position.getFixTime());
        Assert.assertTrue("year > 2000", position.getFixTime().after(new Date(946684800000L)));
        Assert.assertTrue("time < +25 hours",
                position.getFixTime().getTime() < System.currentTimeMillis() + 25 * 3600000);

        Assert.assertTrue("latitude >= -90", position.getLatitude() >= -90);
        Assert.assertTrue("latitude <= 90", position.getLatitude() <= 90);

        Assert.assertTrue("longitude >= -180", position.getLongitude() >= -180);
        Assert.assertTrue("longitude <= 180", position.getLongitude() <= 180);

        Assert.assertTrue("altitude >= -12262", position.getAltitude() >= -12262);
        Assert.assertTrue("altitude <= 18000", position.getAltitude() <= 18000);

        Assert.assertTrue("speed >= 0", position.getSpeed() >= 0);
        Assert.assertTrue("speed <= 869", position.getSpeed() <= 869);

        Assert.assertTrue("course >= 0", position.getCourse() >= 0);
        Assert.assertTrue("course <= 360", position.getCourse() <= 360);

    }

}
