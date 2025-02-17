/*
 * Copyright 2013 - 2024 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.config.Keys;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DataConverter;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AtrackProtocolDecoder extends BaseProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtrackProtocolDecoder.class);

    /**
     * Minimum length for the fixed portion of a single binary record in AX-series.
     * Is set to 34 to ensure there is enough bytes for 3 timestamps (12 bytes) plus
     * at least ~22 bytes of the other fixed fields. (Driver ID and text are variable-length.)
     */
    private static final int MIN_DATA_LENGTH = 34;

    private boolean longDate;
    private boolean decimalFuel;
    private boolean custom;
    private String form;

    private ByteBuf photo;

    private final Map<Integer, String> alarmMap = new HashMap<>();

    public AtrackProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected void init() {
        longDate = getConfig().getBoolean(Keys.PROTOCOL_LONG_DATE.withPrefix(getProtocolName()));
        decimalFuel = getConfig().getBoolean(Keys.PROTOCOL_DECIMAL_FUEL.withPrefix(getProtocolName()));

        custom = getConfig().getBoolean(Keys.PROTOCOL_CUSTOM.withPrefix(getProtocolName()));
        form = getConfig().getString(Keys.PROTOCOL_FORM.withPrefix(getProtocolName()));
        if (form != null) {
            custom = true;
        }

        String alarmMapString = getConfig().getString(Keys.PROTOCOL_ALARM_MAP.withPrefix(getProtocolName()));
        if (alarmMapString != null) {
            for (String pair : alarmMapString.split(",")) {
                if (!pair.isEmpty()) {
                    alarmMap.put(
                            Integer.parseInt(pair.substring(0, pair.indexOf('='))),
                            pair.substring(pair.indexOf('=') + 1));
                }
            }
        }
    }

    public void setLongDate(boolean longDate) {
        this.longDate = longDate;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public void setForm(String form) {
        this.form = form;
    }

    private void sendResponse(Channel channel, SocketAddress remoteAddress, long rawId, int index) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer(12);
            response.writeShort(0xfe02);
            response.writeLong(rawId);
            response.writeShort(index);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    /**
     * Reads a null-terminated ASCII string from the buffer.
     * If the buffer doesn't contain a null terminator before writerIndex, returns everything.
     */
    private static String readString(ByteBuf buf) {
        String result = null;
        int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) 0);
        if (index > buf.readerIndex()) {
            result = buf.readSlice(index - buf.readerIndex()).toString(StandardCharsets.US_ASCII);
        }
        // consume trailing zero if available
        if (buf.isReadable()) {
            buf.readByte();
        }
        return result;
    }

    /**
     * Decode BLE or iBeacon extended data from custom fields (if present).
     */
    private void decodeBeaconData(Position position, int mode, int mask, ByteBuf data) {
        int i = 1;
        while (data.isReadable()) {
            if (BitUtil.check(mask, 7)) {
                position.set("tag" + i + "Id", ByteBufUtil.hexDump(data.readSlice(6)));
            }
            switch (mode) {
                case 1 -> {
                    if (BitUtil.check(mask, 6)) {
                        data.readUnsignedShort(); // major
                    }
                    if (BitUtil.check(mask, 5)) {
                        data.readUnsignedShort(); // minor
                    }
                    if (BitUtil.check(mask, 4)) {
                        data.readUnsignedByte(); // tx power
                    }
                    if (BitUtil.check(mask, 3)) {
                        position.set("tag" + i + "Rssi", data.readUnsignedByte());
                    }
                }
                case 2 -> {
                    if (BitUtil.check(mask, 6)) {
                        data.readUnsignedShort(); // battery voltage
                    }
                    if (BitUtil.check(mask, 5)) {
                        position.set("tag" + i + "Temp", data.readUnsignedShort());
                    }
                    if (BitUtil.check(mask, 4)) {
                        data.readUnsignedByte(); // tx power
                    }
                    if (BitUtil.check(mask, 3)) {
                        position.set("tag" + i + "Rssi", data.readUnsignedByte());
                    }
                }
                case 3 -> {
                    if (BitUtil.check(mask, 6)) {
                        position.set("tag" + i + "Humidity", data.readUnsignedShort());
                    }
                    if (BitUtil.check(mask, 5)) {
                        position.set("tag" + i + "Temp", data.readUnsignedShort());
                    }
                    if (BitUtil.check(mask, 3)) {
                        position.set("tag" + i + "Rssi", data.readUnsignedByte());
                    }
                    if (BitUtil.check(mask, 2)) {
                        data.readUnsignedShort(); // battery or sensor
                    }
                }
                case 4 -> {
                    if (BitUtil.check(mask, 6)) {
                        int hardwareId = data.readUnsignedByte();
                        if (BitUtil.check(mask, 5)) {
                            switch (hardwareId) {
                                case 1, 4 -> data.skipBytes(11); // fuel
                                case 2 -> data.skipBytes(2);  // temperature
                                case 3 -> data.skipBytes(6);  // temperature + luminosity
                                case 5 -> data.skipBytes(10); // temperature, humidity, luminosity, pressure
                            }
                        }
                    }
                    if (BitUtil.check(mask, 4)) {
                        data.skipBytes(9);
                    }
                }
            }
            i += 1;
        }
    }

    /**
     * Reads extra textual custom data fields if the user configured `custom=true`.
     */
    private void readTextCustomData(Position position, String data, String form) {
        CellTower cellTower = new CellTower();
        String[] keys = form.substring(1).split("%");
        String[] values = data.split(",|\r\n");
        for (int i = 0; i < Math.min(keys.length, values.length); i++) {
            switch (keys[i]) {
                case "SA" -> position.set(Position.KEY_SATELLITES, Integer.parseInt(values[i]));
                case "MV" -> position.set(Position.KEY_POWER, Integer.parseInt(values[i]) * 0.1);
                case "BV" -> position.set(Position.KEY_BATTERY, Integer.parseInt(values[i]) * 0.1);
                case "GQ" -> cellTower.setSignalStrength(Integer.parseInt(values[i]));
                case "CE" -> cellTower.setCellId(Long.parseLong(values[i]));
                case "LC" -> cellTower.setLocationAreaCode(Integer.parseInt(values[i]));
                case "CN" -> {
                    if (values[i].length() > 3) {
                        cellTower.setMobileCountryCode(Integer.parseInt(values[i].substring(0, 3)));
                        cellTower.setMobileNetworkCode(Integer.parseInt(values[i].substring(3)));
                    }
                }
                case "PC" -> position.set(Position.PREFIX_COUNT + 1, Integer.parseInt(values[i]));
                case "AT" -> position.setAltitude(Integer.parseInt(values[i]));
                case "RP" -> position.set(Position.KEY_RPM, Integer.parseInt(values[i]));
                case "GS" -> position.set(Position.KEY_RSSI, Integer.parseInt(values[i]));
                case "DT" -> position.set(Position.KEY_ARCHIVE, Integer.parseInt(values[i]) == 1);
                case "VN" -> position.set(Position.KEY_VIN, values[i]);
                case "TR" -> position.set(Position.KEY_THROTTLE, Integer.parseInt(values[i]));
                case "ET" -> position.set(Position.KEY_COOLANT_TEMP, Integer.parseInt(values[i]));
                case "FL" -> position.set(Position.KEY_FUEL_LEVEL, Integer.parseInt(values[i]));
                case "FC" -> position.set(Position.KEY_FUEL_CONSUMPTION, Integer.parseInt(values[i]));
                case "AV1" -> position.set(Position.PREFIX_ADC + 1, Integer.parseInt(values[i]));
                case "CD" -> position.set(Position.KEY_ICCID, values[i]);
                case "EH" -> position.set(
                        Position.KEY_HOURS,
                        UnitsConverter.msFromHours(Integer.parseInt(values[i]) * 0.1)
                );
                case "IA" -> position.set("intakeTemp", Integer.parseInt(values[i]));
                case "EL" -> position.set(Position.KEY_ENGINE_LOAD, Integer.parseInt(values[i]));
                case "HA" -> {
                    if (Integer.parseInt(values[i]) > 0) {
                        position.addAlarm(Position.ALARM_ACCELERATION);
                    }
                }
                case "HB" -> {
                    if (Integer.parseInt(values[i]) > 0) {
                        position.addAlarm(Position.ALARM_BRAKING);
                    }
                }
                case "HC" -> {
                    if (Integer.parseInt(values[i]) > 0) {
                        position.addAlarm(Position.ALARM_CORNERING);
                    }
                }
                case "MT" -> position.set(Position.KEY_MOTION, Integer.parseInt(values[i]) > 0);
                case "BC" -> {
                    String[] beaconValues = values[i].split(":");
                    if (beaconValues.length >= 3) {
                        int mode = Integer.parseInt(beaconValues[0]);
                        int mask = Integer.parseInt(beaconValues[1]);
                        ByteBuf beaconBuf = Unpooled.wrappedBuffer(DataConverter.parseHex(beaconValues[2]));
                        decodeBeaconData(position, mode, mask, beaconBuf);
                    }
                }
                default -> {
                    // skip unknown
                }
            }
        }

        // Fill in cell tower data if complete
        if (cellTower.getMobileCountryCode() != null
                && cellTower.getMobileNetworkCode() != null
                && cellTower.getCellId() != null
                && cellTower.getLocationAreaCode() != null) {
            position.setNetwork(new Network(cellTower));
        } else if (cellTower.getSignalStrength() != null) {
            position.set(Position.KEY_RSSI, cellTower.getSignalStrength());
        }
    }

    /**
     * Parse extra custom data if the user configured custom=true (binary).
     */
    private void readBinaryCustomData(Position position, ByteBuf buf, String form) {
        CellTower cellTower = new CellTower();
        String[] keys = form.substring(1).split("%");
        for (String key : keys) {
            switch (key) {
                case "SA" -> position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                case "MT" -> position.set(Position.KEY_MOTION, buf.readUnsignedByte() > 0);
                case "MV" -> position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.1);
                case "BV" -> position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.1);
                case "GQ" -> cellTower.setSignalStrength(buf.readUnsignedByte());
                case "CE" -> cellTower.setCellId(buf.readUnsignedInt());
                case "LC" -> cellTower.setLocationAreaCode(buf.readUnsignedShort());
                case "CN" -> {
                    // e.g. 310410 => MCC=310, MNC=410
                    int combinedMobileCodes = (int) (buf.readUnsignedInt() % 100000);
                    cellTower.setMobileCountryCode(combinedMobileCodes / 100);
                    cellTower.setMobileNetworkCode(combinedMobileCodes % 100);
                }
                case "RL" -> buf.readUnsignedByte(); // skip
                case "PC" -> position.set(Position.PREFIX_COUNT + 1, buf.readUnsignedInt());
                case "AT" -> position.setAltitude(buf.readUnsignedInt());
                case "RP" -> position.set(Position.KEY_RPM, buf.readUnsignedShort());
                case "GS" -> position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                case "DT" -> position.set(Position.KEY_ARCHIVE, buf.readUnsignedByte() == 1);
                case "VN" -> position.set(Position.KEY_VIN, readString(buf));
                case "MF" -> buf.readUnsignedShort(); // skip
                case "EL" -> buf.readUnsignedByte();  // skip
                case "TR" -> position.set(Position.KEY_THROTTLE, buf.readUnsignedByte());
                case "ET" -> position.set(Position.PREFIX_TEMP + 1, buf.readUnsignedShort());
                case "FL" -> position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedByte());
                case "ML" -> buf.readUnsignedByte(); // skip
                case "FC" -> position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedInt());
                case "CI" -> readString(buf); // custom info string
                case "AV1" -> position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort());
                case "NC" -> readString(buf); // neighbor cell info
                case "SM" -> buf.readUnsignedShort(); // skip
                case "GL" -> readString(buf); // google link
                case "MA" -> readString(buf); // mac address
                case "PD" -> buf.readUnsignedByte();  // skip
                case "CD" -> position.set(Position.KEY_ICCID, readString(buf));
                case "CM" -> buf.readLong(); // imsi
                case "GN" -> buf.skipBytes(60); // skip g-sensor
                case "GV" -> buf.skipBytes(6);  // skip max g
                case "ME" -> buf.readLong();    // imei
                case "IA" -> buf.readUnsignedByte(); // intake air
                case "MP" -> buf.readUnsignedByte(); // manifold abs press
                case "EO" -> position.set(Position.KEY_ODOMETER, UnitsConverter.metersFromMiles(buf.readUnsignedInt()));
                case "EH" -> position.set(Position.KEY_HOURS, buf.readUnsignedInt() * 360000);
                case "ZO1" -> buf.readUnsignedByte(); // brake stroke status
                case "ZO2" -> buf.readUnsignedByte(); // warning indicator status
                case "ZO3" -> buf.readUnsignedByte(); // abs control status
                case "ZO4" -> position.set(Position.KEY_THROTTLE, buf.readUnsignedByte() * 0.4);
                case "ZO5" -> buf.readUnsignedByte(); // parking brake status
                case "ZO6" -> position.set(Position.KEY_OBD_SPEED, buf.readUnsignedByte() * 0.805);
                case "ZO7" -> buf.readUnsignedByte(); // cruise control status
                case "ZO8" -> buf.readUnsignedByte(); // accelector pedal position
                case "ZO9" -> position.set(Position.KEY_ENGINE_LOAD, buf.readUnsignedByte() * 0.5);
                case "ZO10" -> position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedByte() * 0.5);
                case "ZO11" -> buf.readUnsignedByte(); // engine oil pressure
                case "ZO12" -> buf.readUnsignedByte(); // boost pressure
                case "ZO13" -> buf.readUnsignedByte(); // intake temperature
                case "ZO14" -> position.set(Position.KEY_COOLANT_TEMP, buf.readUnsignedByte());
                case "ZO15" -> buf.readUnsignedByte(); // brake application pressure
                case "ZO16" -> buf.readUnsignedByte(); // brake primary pressure
                case "ZO17" -> buf.readUnsignedByte(); // brake secondary pressure
                case "ZH1" -> buf.readUnsignedShort(); // cargo weight
                case "ZH2" -> position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedShort() * 16.428 / 3600);
                case "ZH3" -> position.set(Position.KEY_RPM, buf.readUnsignedShort() * 0.25);
                case "ZL1" -> buf.readUnsignedInt(); // fuel used (natural gas)
                case "ZL2" -> position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 161);
                case "ZL3" -> buf.readUnsignedInt(); // vehicle hours
                case "ZL4" -> position.set(Position.KEY_HOURS, buf.readUnsignedInt() * 5 * 36000);
                case "ZS1" -> position.set(Position.KEY_VIN, readString(buf));
                case "JO1" -> buf.readUnsignedByte(); // pedals
                case "JO2" -> buf.readUnsignedByte(); // power takeoff device
                case "JO3" -> buf.readUnsignedByte(); // accelector pedal position
                case "JO4" -> position.set(Position.KEY_ENGINE_LOAD, buf.readUnsignedByte());
                case "JO5" -> position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedByte() * 0.4);
                case "JO6" -> buf.readUnsignedByte(); // fms vehicle interface
                case "JO7" -> buf.readUnsignedByte(); // driver 2
                case "JO8" -> buf.readUnsignedByte(); // driver 1
                case "JO9" -> buf.readUnsignedByte(); // drivers
                case "JO10" -> buf.readUnsignedByte(); // system information
                case "JO11" -> position.set(Position.KEY_COOLANT_TEMP, buf.readUnsignedByte() - 40);
                case "JO12" -> buf.readUnsignedByte(); // pto engaged
                case "JH1" -> position.set(Position.KEY_OBD_SPEED, buf.readUnsignedShort() / 256.0);
                case "JH2" -> position.set(Position.KEY_RPM, buf.readUnsignedShort() * 0.125);
                case "JH3", "JH4", "JH5", "JH6", "JH7" -> {
                    int index = Integer.parseInt(key.substring(2)) - 2;
                    position.set("axleWeight" + index, buf.readUnsignedShort() * 0.5);
                }
                case "JH8" -> position.set(Position.KEY_ODOMETER_SERVICE, buf.readUnsignedShort() * 5);
                case "JH9" -> buf.readUnsignedShort(); // tachograph speed
                case "JH10" -> buf.readUnsignedShort(); // ambient air temperature
                case "JH11" -> position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedShort() * 0.05);
                case "JH12" -> buf.readUnsignedShort(); // fuel economy
                case "JL1" -> position.set(Position.KEY_FUEL_USED, buf.readUnsignedInt() * 0.5);
                case "JL2" -> position.set(Position.KEY_HOURS, buf.readUnsignedInt() * 5 * 36000);
                case "JL3" -> position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 1000);
                case "JL4" -> position.set(Position.KEY_FUEL_USED, buf.readUnsignedInt() * 0.001);
                case "JS1" -> position.set(Position.KEY_VIN, readString(buf));
                case "JS2" -> readString(buf); // fms version supported
                case "JS3" -> position.set("driver1", readString(buf));
                case "JS4" -> position.set("driver2", readString(buf));
                case "JN1" -> buf.readUnsignedInt(); // cruise control distance
                case "JN2" -> buf.readUnsignedInt(); // excessive idling time
                case "JN3" -> buf.readUnsignedInt(); // excessive idling fuel
                case "JN4" -> buf.readUnsignedInt(); // pto time
                case "JN5" -> buf.readUnsignedInt(); // pto fuel
                case "IN0" -> position.set(Position.KEY_IGNITION, buf.readUnsignedByte() > 0);
                case "IN1", "IN2", "IN3" -> {
                    position.set(Position.PREFIX_IN + key.charAt(2), buf.readUnsignedByte() > 0);
                }
                case "HA" -> {
                    if (buf.readUnsignedByte() > 0) {
                        position.addAlarm(Position.ALARM_ACCELERATION);
                    }
                }
                case "HB" -> {
                    if (buf.readUnsignedByte() > 0) {
                        position.addAlarm(Position.ALARM_BRAKING);
                    }
                }
                case "HC" -> {
                    if (buf.readUnsignedByte() > 0) {
                        position.addAlarm(Position.ALARM_CORNERING);
                    }
                }
                default -> {
                    // skip unknown
                }
            }
        }

        if (cellTower.getMobileCountryCode() != null
                && cellTower.getMobileNetworkCode() != null
                && cellTower.getCellId() != null
                && cellTower.getCellId() != 0
                && cellTower.getLocationAreaCode() != null) {
            position.setNetwork(new Network(cellTower));
        } else if (cellTower.getSignalStrength() != null) {
            position.set(Position.KEY_RSSI, cellTower.getSignalStrength());
        }
    }

    private static final Pattern PATTERN_INFO = new PatternBuilder()
            .text("$INFO=")
            .number("(d+),")                     // unit id
            .expression("([^,]+),")              // model
            .expression("([^,]+),")              // firmware version
            .number("d+,")                       // imei
            .number("d+,")                       // imsi
            .number("d+,")                       // sim card id
            .number("(d+),")                     // power
            .number("(d+),")                     // battery
            .number("(d+),")                     // satellites
            .number("d+,")                       // gsm status
            .number("(d+),")                     // rssi
            .number("d+,")                       // connection status
            .number("d+")                        // antenna status
            .any()
            .compile();

    /**
     * Decodes $INFO= or other $-prefixed “at commands”.
     */
    private Position decodeInfo(Channel channel, SocketAddress remoteAddress, String sentence) {

        Position position = new Position(getProtocolName());
        getLastLocation(position, null);

        DeviceSession deviceSession;

        if (sentence.startsWith("$INFO")) {

            Parser parser = new Parser(PATTERN_INFO, sentence);
            if (!parser.matches()) {
                return null;
            }

            deviceSession = getDeviceSession(channel, remoteAddress, parser.next());

            position.set("model", parser.next());
            position.set(Position.KEY_VERSION_FW, parser.next());
            position.set(Position.KEY_POWER, parser.nextInt() * 0.1);
            position.set(Position.KEY_BATTERY, parser.nextInt() * 0.1);
            position.set(Position.KEY_SATELLITES, parser.nextInt());
            position.set(Position.KEY_RSSI, parser.nextInt());

        } else {
            // if not $INFO, treat as generic response
            deviceSession = getDeviceSession(channel, remoteAddress);
            position.set(Position.KEY_RESULT, sentence);
        }

        if (deviceSession == null) {
            return null;
        } else {
            position.setDeviceId(deviceSession.getDeviceId());
            return position;
        }
    }

    // Pattern for text-based lines
    private static final Pattern PATTERN = new PatternBuilder()
            .number("(d+),")                    // gps date/time
            .number("d+,")                      // rtc date/time
            .number("d+,")                      // send date/time
            .number("(-?d+),")                  // lon
            .number("(-?d+),")                  // lat
            .number("(d+),")                    // course
            .number("(d+),")                    // report id
            .number("(d+.?d*),")                // odometer
            .number("(d+),")                    // hdop
            .number("(d+),")                    // inputs
            .number("(d+),")                    // speed
            .number("(d+),")                    // outputs
            .number("(d+),")                    // adc
            .number("([^,]+)?,")                // driver
            .number("(d+),")                    // temp1
            .number("(d+),")                    // temp2
            .expression("[^,]*,")               // text message
            .expression("(.*)")                 // custom data
            .optional(2)
            .compile();

    /**
     * Decodes a single line of ASCII data.
     */
    private Position decodeTextLine(DeviceSession deviceSession, String sentence) {
        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        // parse date/time
        String timeValue = parser.next();
        Date fixTime;
        if (timeValue.length() >= 14) {
            // e.g. 20250108092241
            try {
                DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                fixTime = dateFormat.parse(timeValue);
            } catch (ParseException e) {
                LOGGER.warn("Skipping text record: invalid time {}", timeValue, e);
                return null;
            }
        } else {
            fixTime = new Date(Long.parseLong(timeValue) * 1000);
        }

        double rawLon = parser.nextInt() * 0.000001;
        double rawLat = parser.nextInt() * 0.000001;

        if (rawLon < -180 || rawLon > 180) {
            LOGGER.warn("Skipping text record: invalid longitude {}", rawLon);
            return null;
        }
        if (rawLat < -90 || rawLat > 90) {
            LOGGER.warn("Skipping text record: invalid latitude {}", rawLat);
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setValid(true);
        position.setTime(fixTime);
        position.setLongitude(rawLon);
        position.setLatitude(rawLat);

        position.setCourse(parser.nextInt());
        position.set(Position.KEY_EVENT, parser.nextInt());
        position.set(Position.KEY_ODOMETER, parser.nextDouble() * 100);
        position.set(Position.KEY_HDOP, parser.nextInt() * 0.1);
        position.set(Position.KEY_INPUT, parser.nextInt());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
        position.set(Position.KEY_OUTPUT, parser.nextInt());
        position.set(Position.PREFIX_ADC + 1, parser.nextInt());

        if (parser.hasNext()) {
            position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());
        }
        position.set(Position.PREFIX_TEMP + 1, parser.nextInt());
        position.set(Position.PREFIX_TEMP + 2, parser.nextInt());

        // custom data if present
        if (custom && parser.hasNext()) {
            String data = parser.next();
            String localForm = this.form;
            if (localForm == null) {
                // possibly parse from leading %CI
                if (data.startsWith("%CI")) {
                    int commaIndex = data.indexOf(',');
                    if (commaIndex > 0) {
                        localForm = data.substring(0, commaIndex).substring("%CI".length());
                        data = data.substring(commaIndex + 1);
                    }
                }
            }
            if (localForm != null) {
                readTextCustomData(position, data, localForm);
            }
        }

        return position;
    }

    /**
     * Decodes an entire ASCII message that may contain multiple lines.
     */
    private List<Position> decodeText(Channel channel, SocketAddress remoteAddress, String sentence) {

        // parse header first (5 commas)
        int positionIndex = -1;
        for (int i = 0; i < 5; i++) {
            positionIndex = sentence.indexOf(',', positionIndex + 1);
        }
        if (positionIndex < 0) {
            LOGGER.warn("Invalid text message, cannot find 5th comma: {}", sentence);
            return null;
        }

        String[] headers = sentence.substring(0, positionIndex).split(",");
        long id = Long.parseLong(headers[2]);
        int index = Integer.parseInt(headers[3]);

        // get device using 5th field
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, headers[4]);
        if (deviceSession == null) {
            return null;
        }

        // ack
        sendResponse(channel, remoteAddress, id, index);

        // lines after that
        List<Position> positions = new LinkedList<>();
        String[] lines = sentence.substring(positionIndex + 1).split("\r\n");

        for (String line : lines) {
            if (!line.isBlank()) {
                Position position = decodeTextLine(deviceSession, line);
                if (position != null) {
                    positions.add(position);
                }
            }
        }

        return positions;
    }

    /**
     * Decode photo frames (@R).
     */
    private Position decodePhoto(DeviceSession deviceSession, ByteBuf buf, long id) {

        long time = buf.readUnsignedInt();
        int index = buf.readUnsignedByte();
        int count = buf.readUnsignedByte();

        int length = buf.readUnsignedShort();
        if (photo == null) {
            photo = Unpooled.buffer();
        }

        photo.writeBytes(buf.readSlice(length));

        if (index == count - 1) {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            getLastLocation(position, new Date(time * 1000));
            position.set(Position.KEY_IMAGE, writeMediaFile(String.valueOf(id), photo, "jpg"));

            photo.release();
            photo = null;
            return position;
        }

        return null;
    }

    /**
     * Parses exactly one binary record (3 timestamps, lat/lon, heading, etc.),
     * including variable driver ID and text. Returns null if we don't have enough bytes
     * to parse a complete record.
     */
    private Position parseBinaryRecord(DeviceSession deviceSession, ByteBuf buf) {

        if (buf.readableBytes() < MIN_DATA_LENGTH) {
            return null; // not enough data
        }

        // parse timestamps
        Date fixTime;
        if (longDate) {
            // year,month,day,hour,min,sec => 6 bytes
            if (buf.readableBytes() < 6) {
                return null;
            }
            DateBuilder dateBuilder = new DateBuilder()
                    .setDate(buf.readUnsignedShort(), buf.readUnsignedByte(), buf.readUnsignedByte())
                    .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
            fixTime = dateBuilder.getDate();

            // skip next 14 for deviceTime + sendTime
            if (buf.readableBytes() < 14) {
                return null;
            }
            buf.skipBytes(14);

        } else {
            // 3 x 4-byte Unix times => 12 bytes total
            if (buf.readableBytes() < 12) {
                return null;
            }
            long gpsTime = buf.readUnsignedInt();
            fixTime = new Date(gpsTime * 1000);
            buf.skipBytes(8); // device + send time
        }

        // next 8 = lon, lat
        if (buf.readableBytes() < 8) {
            return null;
        }
        double rawLon = buf.readInt() * 0.000001;
        double rawLat = buf.readInt() * 0.000001;

        if (rawLon < -180 || rawLon > 180 || rawLat < -90 || rawLat > 90) {
            LOGGER.warn("Skipping record: invalid lat/lon ({}, {})", rawLat, rawLon);
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(fixTime);
        position.setLongitude(rawLon);
        position.setLatitude(rawLat);
        position.setValid(true);

        // heading
        if (buf.readableBytes() < 2) {
            return null;
        }
        position.setCourse(buf.readUnsignedShort());

        // report type
        if (buf.readableBytes() < 1) {
            return null;
        }
        int type = buf.readUnsignedByte();
        position.set(Position.KEY_TYPE, type);
        if (alarmMap.containsKey(type)) {
            position.addAlarm(alarmMap.get(type));
        }

        // odometer
        if (buf.readableBytes() < 4) {
            return null;
        }
        position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 100);

        // hdop
        if (buf.readableBytes() < 2) {
            return null;
        }
        position.set(Position.KEY_HDOP, buf.readUnsignedShort() * 0.1);

        // inputs
        if (buf.readableBytes() < 1) {
            return null;
        }
        position.set(Position.KEY_INPUT, buf.readUnsignedByte());

        // speed
        if (buf.readableBytes() < 2) {
            return null;
        }
        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));

        // outputs
        if (buf.readableBytes() < 1) {
            return null;
        }
        position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());

        // analog
        if (buf.readableBytes() < 2) {
            return null;
        }
        position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort() * 0.001);

        // driver ID (null-terminated)
        String driverId = readString(buf);
        if (driverId != null && !driverId.isEmpty()) {
            position.set(Position.KEY_DRIVER_UNIQUE_ID, driverId);
        }

        // temp1, temp2
        if (buf.readableBytes() < 4) {
            return null;
        }
        position.set(Position.PREFIX_TEMP + 1, buf.readShort() * 0.1);
        position.set(Position.PREFIX_TEMP + 2, buf.readShort() * 0.1);

        // text (null-terminated)
        if (buf.isReadable()) {
            String message = readString(buf);
            if (message != null && !message.isEmpty()) {
                // check if it matches FULS pattern
                Pattern pattern = Pattern.compile("FULS:F=(\\p{XDigit}+) t=(\\p{XDigit}+) N=(\\p{XDigit}+)");
                Matcher matcher = pattern.matcher(message);
                if (matcher.find()) {
                    int value = Integer.parseInt(matcher.group(3), decimalFuel ? 10 : 16);
                    position.set(Position.KEY_FUEL_LEVEL, value * 0.1);
                } else {
                    position.set("message", message);
                }
            }
        }

        // custom data (if any)
        if (custom && buf.isReadable()) {
            int savedIndex = buf.readerIndex();
            String localForm = readString(buf);
            if (localForm != null && localForm.startsWith("%CI")) {
                localForm = localForm.substring("%CI".length()).trim();
                readBinaryCustomData(position, buf, localForm);
            } else {
                // revert if there's no %CI
                buf.readerIndex(savedIndex);
            }
        }

        return position;
    }

    /**
     * Decode binary frames that might contain multiple positions in one packet.
     */
    private List<Position> decodeBinary(DeviceSession deviceSession, ByteBuf buf) {
        List<Position> positions = new LinkedList<>();

        while (true) {
            int startIndex = buf.readerIndex();
            Position position = parseBinaryRecord(deviceSession, buf);
            if (position == null) {
                buf.readerIndex(startIndex);
                break;
            }
            positions.add(position);
        }

        return positions;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        try {
            // keep-alive?
            if (buf.getUnsignedShort(buf.readerIndex()) == 0xfe02) {
                if (channel != null) {
                    // echo back
                    channel.writeAndFlush(new NetworkMessage(buf.retain(), remoteAddress));
                }
                return null;
            }

            // check if $-prefixed ASCII
            if (buf.getByte(buf.readerIndex()) == '$') {
                return decodeInfo(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII).trim());
            }

            // text-based if the 3rd byte is ','
            if (buf.getByte(buf.readerIndex() + 2) == ',') {
                return decodeText(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII).trim());
            }

            // otherwise parse binary
            String prefix = buf.readCharSequence(2, StandardCharsets.US_ASCII).toString();
            buf.readUnsignedShort(); 
            int length = buf.readUnsignedShort(); // data length
            int index = buf.readUnsignedShort();  // seq ID
            long id = buf.readLong();             // unit ID

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(id));
            if (deviceSession == null) {
                return null;
            }

            // ack
            sendResponse(channel, remoteAddress, id, index);

            if (prefix.equals("@R")) {
                // handle photo frames
                return decodePhoto(deviceSession, buf, id);
            } else {
                // parse standard binary positions
                return decodeBinary(deviceSession, buf);
            }

        } catch (Exception e) {
            LOGGER.error("Decoding error: {}", e.getMessage(), e);

            // Try to ack if possible
            try {
                if (buf.readableBytes() >= 14) {
                    int savedReaderIndex = buf.readerIndex();
                    String prefix = buf.readCharSequence(2, StandardCharsets.US_ASCII).toString();
                    buf.readUnsignedShort(); // skip
                    buf.readUnsignedShort(); // length
                    int index = buf.readUnsignedShort();
                    long id = buf.readLong();
                    sendResponse(channel, remoteAddress, id, index);
                    buf.readerIndex(savedReaderIndex);
                }
            } catch (Exception ackEx) {
                LOGGER.warn("Failed to parse ID/index for ack after error: {}", ackEx.getMessage());
            }
            return null;
        }
    }

}
