package org.traccar;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.AfterEach;
import org.traccar.helper.DataConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Command;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProtocolTest extends BaseTest {

    private final List<EmbeddedChannel> channels = new ArrayList<>();

    protected EmbeddedChannel channel(ChannelHandler... handlers) {
        var channel = new EmbeddedChannel(handlers);
        channels.add(channel);
        return channel;
    }

    @AfterEach
    public void closeChannels() {
        for (var channel : channels) {
            channel.finishAndReleaseAll();
        }
        channels.clear();
    }

    protected enum Checks {
        ALL, ATTRIBUTES, NONE
    }

    protected PositionExpectation position() {
        return position(Checks.ALL);
    }

    protected PositionExpectation position(Checks mode) {
        return new PositionExpectation(mode);
    }

    protected PositionExpectation[] positions(int count) {
        return positions(count, Checks.ALL);
    }

    protected PositionExpectation[] positions(int count, Checks mode) {
        var expected = new PositionExpectation[count];
        for (int i = 0; i < count; i++) {
            expected[i] = position(mode);
        }
        return expected;
    }

    protected NetworkExpectation network() {
        return new NetworkExpectation();
    }

    protected CellTowerExpectation cell() {
        return new CellTowerExpectation();
    }

    protected WifiAccessPointExpectation wifi() {
        return new WifiAccessPointExpectation();
    }

    protected void verify(
            BaseProtocolDecoder decoder, Object object, PositionExpectation... expected) throws Exception {
        Object decoded = decoder.decode(null, null, object);
        List<?> positions = switch (decoded) {
            case null -> List.of();
            case Position position -> List.of(position);
            default -> assertInstanceOf(List.class, decoded, "positions");
        };
        assertEquals(expected.length, positions.size(), "positions.count");
        for (int i = 0; i < positions.size(); i++) {
            String path = "position[" + i + "]";
            var actual = assertInstanceOf(Position.class, positions.get(i), path);
            expected[i].verify(actual, path);
        }
    }

    protected void verify(EmbeddedChannel channel, ByteBuf input, ByteBuf... expected) {
        channel.writeInbound(input);
        assertEquals(expected.length, channel.inboundMessages().size(), "frames.count");
        for (int i = 0; i < expected.length; i++) {
            String path = "frame[" + i + "]";
            var actual = assertInstanceOf(ByteBuf.class, channel.readInbound(), path);
            assertEquals(ByteBufUtil.hexDump(expected[i]), ByteBufUtil.hexDump(actual), path);
        }
    }

    protected void verify(EmbeddedChannel channel, Command command, Object... expected) {
        channel.writeOutbound(new NetworkMessage(command, null));
        assertEquals(expected.length, channel.outboundMessages().size(), "messages.count");
        for (int i = 0; i < expected.length; i++) {
            String path = "message[" + i + "]";
            var message = assertInstanceOf(NetworkMessage.class, channel.readOutbound(), path);
            if (expected[i] instanceof ByteBuf buffer) {
                var actual = assertInstanceOf(ByteBuf.class, message.getMessage(), path);
                assertEquals(ByteBufUtil.hexDump(buffer), ByteBufUtil.hexDump(actual), path);
            } else {
                assertEquals(expected[i], message.getMessage(), path);
            }
        }
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

    protected DefaultFullHttpRequest request(HttpMethod method, String url, HttpHeaders headers, ByteBuf data) {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, url, data, headers, new DefaultHttpHeaders());
    }

    protected DefaultFullHttpResponse response(ByteBuf data) {
        return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, data);
    }

    protected void verifyCommand(
            BaseProtocolEncoder encoder, Command command, ByteBuf expected) {
        verifyFrame(expected, encoder.encodeCommand(command));
    }

    protected void verifyFrame(ByteBuf expected, Object object) {
        assertNotNull(object, "buffer is null");
        assertInstanceOf(ByteBuf.class, object, "not a buffer");
        assertEquals(ByteBufUtil.hexDump(expected), ByteBufUtil.hexDump((ByteBuf) object));
    }

    public static final class PositionExpectation {

        private final List<BiConsumer<Position, String>> checks = new ArrayList<>();

        private final Checks mode;

        private PositionExpectation(Checks mode) {
            this.mode = mode;
        }

        void verify(Position position, String path) {

            if (mode == Checks.ALL) {

                assertNotNull(position.getServerTime());
                assertNotNull(position.getFixTime());
                assertTrue(position.getFixTime().after(new Date(915148800000L)), "year > 1999");
                assertTrue(position.getFixTime().getTime() < System.currentTimeMillis() + 25 * 3600000, "time < +25 h");

                assertTrue(position.getLatitude() >= -90, "latitude >= -90");
                assertTrue(position.getLatitude() <= 90, "latitude <= 90");

                assertTrue(position.getLongitude() >= -180, "longitude >= -180");
                assertTrue(position.getLongitude() <= 180, "longitude <= 180");

                assertTrue(position.getAltitude() >= -12262, "altitude >= -12262");
                assertTrue(position.getAltitude() <= 18000, "altitude <= 18000");

                assertTrue(position.getSpeed() >= 0, "speed >= 0");
                assertTrue(position.getSpeed() <= 869, "speed <= 869");

                assertTrue(position.getCourse() >= 0, "course >= 0");
                assertTrue(position.getCourse() <= 360, "course <= 360");

                assertNotNull(position.getProtocol(), "protocol is null");

                assertTrue(position.getDeviceId() > 0, "deviceId > 0");

            }

            if (mode != Checks.NONE) {
                Map<String, Object> attributes = position.getAttributes();

                if (mode == Checks.ATTRIBUTES) {
                    assertFalse(attributes.isEmpty(), "no attributes");
                }

                if (attributes.containsKey(Position.KEY_INDEX)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_INDEX));
                }

                if (attributes.containsKey(Position.KEY_HDOP)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_HDOP));
                }

                if (attributes.containsKey(Position.KEY_VDOP)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_VDOP));
                }

                if (attributes.containsKey(Position.KEY_PDOP)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_PDOP));
                }

                if (attributes.containsKey(Position.KEY_SATELLITES)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_SATELLITES));
                }

                if (attributes.containsKey(Position.KEY_SATELLITES_VISIBLE)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_SATELLITES_VISIBLE));
                }

                if (attributes.containsKey(Position.KEY_RSSI)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_RSSI));
                }

                if (attributes.containsKey(Position.KEY_ODOMETER)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_ODOMETER));
                }

                if (attributes.containsKey(Position.KEY_RPM)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_RPM));
                }

                if (attributes.containsKey(Position.KEY_FUEL)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_FUEL));
                }

                if (attributes.containsKey(Position.KEY_FUEL_USED)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_FUEL_USED));
                }

                if (attributes.containsKey(Position.KEY_POWER)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_POWER));
                }

                if (attributes.containsKey(Position.KEY_BATTERY)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_BATTERY));
                }

                if (attributes.containsKey(Position.KEY_BATTERY_LEVEL)) {
                    int batteryLevel = ((Number) attributes.get(Position.KEY_BATTERY_LEVEL)).intValue();
                    assertTrue(batteryLevel <= 100 && batteryLevel >= 0);
                }

                if (attributes.containsKey(Position.KEY_CHARGE)) {
                    assertInstanceOf(Boolean.class, attributes.get(Position.KEY_CHARGE));
                }

                if (attributes.containsKey(Position.KEY_IGNITION)) {
                    assertInstanceOf(Boolean.class, attributes.get(Position.KEY_IGNITION));
                }

                if (attributes.containsKey(Position.KEY_MOTION)) {
                    assertInstanceOf(Boolean.class, attributes.get(Position.KEY_MOTION));
                }

                if (attributes.containsKey(Position.KEY_ARCHIVE)) {
                    assertInstanceOf(Boolean.class, attributes.get(Position.KEY_ARCHIVE));
                }

                if (attributes.containsKey(Position.KEY_DRIVER_UNIQUE_ID)) {
                    assertInstanceOf(String.class, attributes.get(Position.KEY_DRIVER_UNIQUE_ID));
                }

                if (attributes.containsKey(Position.KEY_STEPS)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_STEPS));
                }

                if (attributes.containsKey(Position.KEY_ROAMING)) {
                    assertInstanceOf(Boolean.class, attributes.get(Position.KEY_ROAMING));
                }

                if (attributes.containsKey(Position.KEY_HOURS)) {
                    assertInstanceOf(Number.class, attributes.get(Position.KEY_HOURS));
                }

                if (attributes.containsKey(Position.KEY_RESULT)) {
                    assertInstanceOf(String.class, attributes.get(Position.KEY_RESULT));
                }

                if (position.getNetwork() != null) {
                    if (position.getNetwork().getCellTowers() != null) {
                        for (CellTower cellTower : position.getNetwork().getCellTowers()) {
                            var mcc = cellTower.getMobileCountryCode();
                            assertTrue(mcc != null && mcc >= 0 && mcc <= 999, "mcc: " + mcc);
                            var mnc = cellTower.getMobileNetworkCode();
                            assertTrue(mnc != null && mnc >= 0 && mnc <= 999, "mnc: " + mnc);
                            var lac = cellTower.getLocationAreaCode();
                            assertTrue(lac != null && lac >= 1 && lac <= 65535, "lac: " + lac);
                            var cid = cellTower.getCellId();
                            assertTrue(cid != null && cid >= 0 && cid <= 268435455, "cid: " + cid);
                        }
                    }

                    if (position.getNetwork().getWifiAccessPoints() != null) {
                        for (WifiAccessPoint wifiAccessPoint : position.getNetwork().getWifiAccessPoints()) {
                            var mac = wifiAccessPoint.getMacAddress();
                            assertTrue(mac != null && mac.matches("[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){5}"), "mac: " + mac);
                        }
                    }
                }
            }

            for (var check : checks) {
                check.accept(position, path);
            }

        }

        public PositionExpectation location(String fixTime, boolean valid, double latitude, double longitude) {
            var time = Date.from(Instant.parse(fixTime));
            checks.add((actual, path) -> assertEquals(time, actual.getFixTime(), path + ".fixTime"));
            checks.add((actual, path) -> assertEquals(valid, actual.getValid(), path + ".valid"));
            checks.add((actual, path) -> assertEquals(latitude, actual.getLatitude(), 0.00001, path + ".latitude"));
            checks.add((actual, path) -> assertEquals(longitude, actual.getLongitude(), 0.00001, path + ".longitude"));
            return this;
        }

        public PositionExpectation outdated(boolean expected) {
            checks.add((actual, path) -> assertEquals(expected, actual.getOutdated(), path + ".outdated"));
            return this;
        }

        public PositionExpectation speed(double expected) {
            checks.add((actual, path) -> assertEquals(expected, actual.getSpeed(), 0.00001, path + ".speed"));
            return this;
        }

        public PositionExpectation course(double expected) {
            checks.add((actual, path) -> assertEquals(expected, actual.getCourse(), 0.00001, path + ".course"));
            return this;
        }

        public PositionExpectation altitude(double expected) {
            checks.add((actual, path) -> assertEquals(expected, actual.getAltitude(), 0.00001, path + ".altitude"));
            return this;
        }

        public PositionExpectation accuracy(double expected) {
            checks.add((actual, path) -> assertEquals(expected, actual.getAccuracy(), 0.00001, path + ".accuracy"));
            return this;
        }

        public PositionExpectation deviceTime(String expected) {
            var time = Date.from(Instant.parse(expected));
            checks.add((actual, path) -> assertEquals(time, actual.getDeviceTime(), path + ".deviceTime"));
            return this;
        }

        public PositionExpectation attribute(String key, Object expected) {
            checks.add((actual, path) -> assertEquals(expected, actual.getAttributes().get(key), path + ".attributes." + key));
            return this;
        }

        public PositionExpectation network(Network expected) {
            checks.add((actual, path) -> assertEquals(expected, actual.getNetwork(), path + ".network"));
            return this;
        }

        public PositionExpectation network(NetworkExpectation expected) {
            checks.add((actual, path) -> expected.verify(actual.getNetwork(), path + ".network"));
            return this;
        }

    }

    public static class NetworkExpectation {

        private final List<CellTowerExpectation> towers = new ArrayList<>();
        private final List<WifiAccessPointExpectation> accessPoints = new ArrayList<>();

        public NetworkExpectation cell(CellTowerExpectation expected) {
            towers.add(expected);
            return this;
        }

        public NetworkExpectation wifi(WifiAccessPointExpectation expected) {
            accessPoints.add(expected);
            return this;
        }

        void verify(Network actual, String path) {
            assertNotNull(actual, path);
            if (!towers.isEmpty()) {
                assertNotNull(actual.getCellTowers(), path + ".cellTowers");
                assertEquals(towers.size(), actual.getCellTowers().size(), path + ".cellTowers.count");
                var iterator = actual.getCellTowers().iterator();
                for (int i = 0; i < towers.size(); i++) {
                    var tower = iterator.next();
                    for (var check : towers.get(i).checks) {
                        check.accept(tower, path + ".cellTowers[" + i + "]");
                    }
                }
            }
            if (!accessPoints.isEmpty()) {
                assertNotNull(actual.getWifiAccessPoints(), path + ".wifiAccessPoints");
                assertEquals(accessPoints.size(), actual.getWifiAccessPoints().size(), path + ".wifiAccessPoints.count");
                var iterator = actual.getWifiAccessPoints().iterator();
                for (int i = 0; i < accessPoints.size(); i++) {
                    var accessPoint = iterator.next();
                    for (var check : accessPoints.get(i).checks) {
                        check.accept(accessPoint, path + ".wifiAccessPoints[" + i + "]");
                    }
                }
            }
        }

    }

    public static class CellTowerExpectation {

        private final List<BiConsumer<CellTower, String>> checks = new ArrayList<>();

        public CellTowerExpectation mcc(int expected) {
            checks.add((actual, path) -> assertEquals(expected, actual.getMobileCountryCode(), path + ".mobileCountryCode"));
            return this;
        }

        public CellTowerExpectation mnc(int expected) {
            checks.add((actual, path) -> assertEquals(expected, actual.getMobileNetworkCode(), path + ".mobileNetworkCode"));
            return this;
        }

        public CellTowerExpectation lac(int expected) {
            checks.add((actual, path) -> assertEquals(expected, actual.getLocationAreaCode(), path + ".locationAreaCode"));
            return this;
        }

        public CellTowerExpectation cid(long expected) {
            checks.add((actual, path) -> assertEquals(expected, actual.getCellId(), path + ".cellId"));
            return this;
        }

        public CellTowerExpectation signal(int expected) {
            checks.add((actual, path) -> assertEquals(expected, actual.getSignalStrength(), path + ".signalStrength"));
            return this;
        }

    }

    public static class WifiAccessPointExpectation {

        private final List<BiConsumer<WifiAccessPoint, String>> checks = new ArrayList<>();

        public WifiAccessPointExpectation mac(String expected) {
            checks.add((actual, path) -> assertEquals(expected, actual.getMacAddress(), path + ".macAddress"));
            return this;
        }

        public WifiAccessPointExpectation signal(int expected) {
            checks.add((actual, path) -> assertEquals(expected, actual.getSignalStrength(), path + ".signalStrength"));
            return this;
        }

    }

}
