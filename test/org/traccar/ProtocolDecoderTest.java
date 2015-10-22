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

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

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

    protected Position position(
            Date time, boolean valid, double lat, double lon, double altitude, double speed, double course) {

        Position position = new Position();

        position.setDeviceTime(time);
        position.setFixTime(time);
        position.setValid(valid);
        position.setLatitude(lat);
        position.setLongitude(lon);
        position.setAltitude(altitude);
        position.setSpeed(speed);
        position.setCourse(course);

        return position;
    }

    protected ChannelBuffer binary(String... data) {
        return binary(ByteOrder.BIG_ENDIAN, data);
    }

    protected ChannelBuffer binary(ByteOrder endianness, String... data) {
        return ChannelBuffers.wrappedBuffer(
                endianness, ChannelBufferTools.convertHexString(String.join("", data)));
    }

    protected String text(String... data) {
        return String.join("", data);
    }

    protected ChannelBuffer buffer(String... data) {
        return ChannelBuffers.copiedBuffer(String.join("", data), Charset.defaultCharset());
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

        Assert.assertEquals(position.getDeviceTime(), expected.getDeviceTime());
        Assert.assertEquals(position.getFixTime(), expected.getFixTime());
        Assert.assertEquals(position.getValid(), expected.getValid());
        Assert.assertEquals(position.getLatitude(), expected.getLatitude(), 0.00001);
        Assert.assertEquals(position.getLongitude(), expected.getLongitude(), 0.00001);
        Assert.assertEquals(position.getAltitude(), expected.getAltitude(), 0.01);
        Assert.assertEquals(position.getSpeed(), expected.getSpeed(), 0.01);
        Assert.assertEquals(position.getCourse(), expected.getCourse(), 0.01);

        verifyDecodedPosition(decodedObject);

    }

    private void verifyDecodedPosition(Object decodedObject) {

        Assert.assertNotNull(decodedObject);
        Assert.assertTrue(decodedObject instanceof Position);

        Position position = (Position) decodedObject;

        Assert.assertNotNull(position.getFixTime());
        Assert.assertTrue(position.getFixTime().after(new Date(946684800000L))); // 2000 year
        Assert.assertTrue(position.getFixTime().getTime() < System.currentTimeMillis() + 25 * 3600000); // 25 hours

        Assert.assertTrue(position.getLatitude() >= -90);
        Assert.assertTrue(position.getLatitude() <= 90);

        Assert.assertTrue(position.getLongitude() >= -180);
        Assert.assertTrue(position.getLongitude() <= 180);

        Assert.assertTrue(position.getAltitude() >= -12262);
        Assert.assertTrue(position.getAltitude() <= 18000);

        Assert.assertTrue(position.getSpeed() >= 0);
        Assert.assertTrue(position.getSpeed() <= 869);

        Assert.assertTrue(position.getCourse() >= 0);
        Assert.assertTrue(position.getCourse() <= 360);

    }

}
