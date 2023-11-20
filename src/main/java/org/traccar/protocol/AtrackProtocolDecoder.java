/*
 * Copyright 2013 - 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
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
import org.traccar.protocol.AtrackProtocolDecoderDataReader;

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

    private static final int MIN_DATA_LENGTH = 40;

    private boolean longDate;
    private boolean decimalFuel;
    private boolean custom;
    private String form;

    private ByteBuf photo;

    private final Map<Integer, String> alarmMap = new HashMap<>();

    public AtrackProtocolDecoderDataReader APDDataReader = new AtrackProtocolDecoderDataReader();
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

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(d+),")                     // date and time
            .number("d+,")                       // rtc date and time
            .number("d+,")                       // device date and time
            .number("(-?d+),")                   // longitude
            .number("(-?d+),")                   // latitude
            .number("(d+),")                     // course
            .number("(d+),")                     // report id
            .number("(d+.?d*),")                 // odometer
            .number("(d+),")                     // hdop
            .number("(d+),")                     // inputs
            .number("(d+),")                     // speed
            .number("(d+),")                     // outputs
            .number("(d+),")                     // adc
            .number("([^,]+)?,")                 // driver
            .number("(d+),")                     // temp1
            .number("(d+),")                     // temp2
            .expression("[^,]*,")                // text message
            .expression("(.*)")                  // custom data
            .optional(2)
            .compile();

    private List<Position> decodeText(Channel channel, SocketAddress remoteAddress, String sentence) {

        int positionIndex = -1;
        for (int i = 0; i < 5; i++) {
            positionIndex = sentence.indexOf(',', positionIndex + 1);
        }

        String[] headers = sentence.substring(0, positionIndex).split(",");
        long id = Long.parseLong(headers[2]);
        int index = Integer.parseInt(headers[3]);

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, headers[4]);
        if (deviceSession == null) {
            return null;
        }

        sendResponse(channel, remoteAddress, id, index);

        List<Position> positions = new LinkedList<>();
        String[] lines = sentence.substring(positionIndex + 1).split("\r\n");

        for (String line : lines) {
            Position position = decodeTextLine(deviceSession, line);
            if (position != null) {
                positions.add(position);
            }
        }

        return positions;
    }


    private Position decodeTextLine(DeviceSession deviceSession, String sentence) {

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setValid(true);

        String time = parser.next();
        if (time.length() >= 14) {
            try {
                DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                position.setTime(dateFormat.parse(time));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            position.setTime(new Date(Long.parseLong(time) * 1000));
        }

        position.setLongitude(parser.nextInt() * 0.000001);
        position.setLatitude(parser.nextInt() * 0.000001);
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

        if (custom) {
            String data = parser.next();
            String form = this.form;
            if (form == null) {
                form = data.substring(0, data.indexOf(',')).substring("%CI".length());
                data = data.substring(data.indexOf(',') + 1);
            }
            APDDataReader.readTextCustomData(position, data, form);
        }

        return position;
    }

    private Position decodePhoto(DeviceSession deviceSession, ByteBuf buf, long id) {

        long time = buf.readUnsignedInt();
        int index = buf.readUnsignedByte();
        int count = buf.readUnsignedByte();

        if (photo == null) {
            photo = Unpooled.buffer();
        }
        photo.writeBytes(buf.readSlice(buf.readUnsignedShort()));

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

    private List<Position> decodeBinary(DeviceSession deviceSession, ByteBuf buf) {

        List<Position> positions = new LinkedList<>();

        while (buf.readableBytes() >= MIN_DATA_LENGTH) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            if (longDate) {

                DateBuilder dateBuilder = new DateBuilder()
                        .setDate(buf.readUnsignedShort(), buf.readUnsignedByte(), buf.readUnsignedByte())
                        .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
                position.setTime(dateBuilder.getDate());

                buf.skipBytes(7 + 7);

            } else {
                position.setFixTime(new Date(buf.readUnsignedInt() * 1000));
                position.setDeviceTime(new Date(buf.readUnsignedInt() * 1000));
                buf.readUnsignedInt(); // send time
            }

            position.setValid(true);
            position.setLongitude(buf.readInt() * 0.000001);
            position.setLatitude(buf.readInt() * 0.000001);
            position.setCourse(buf.readUnsignedShort());

            int type = buf.readUnsignedByte();
            position.set(Position.KEY_TYPE, type);
            position.set(Position.KEY_ALARM, alarmMap.get(type));

            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 100);
            position.set(Position.KEY_HDOP, buf.readUnsignedShort() * 0.1);
            position.set(Position.KEY_INPUT, buf.readUnsignedByte());

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));

            position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());
            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort() * 0.001);

            position.set(Position.KEY_DRIVER_UNIQUE_ID, APDDataReader.readString(buf));

            position.set(Position.PREFIX_TEMP + 1, buf.readShort() * 0.1);
            position.set(Position.PREFIX_TEMP + 2, buf.readShort() * 0.1);

            String message = APDDataReader.readString(buf);
            if (message != null && !message.isEmpty()) {
                Pattern pattern = Pattern.compile("FULS:F=(\\p{XDigit}+) t=(\\p{XDigit}+) N=(\\p{XDigit}+)");
                Matcher matcher = pattern.matcher(message);
                if (matcher.find()) {
                    int value = Integer.parseInt(matcher.group(3), decimalFuel ? 10 : 16);
                    position.set(Position.KEY_FUEL_LEVEL, value * 0.1);
                } else {
                    position.set("message", message);
                }
            }

            if (custom) {
                String form = this.form;
                if (form == null) {
                    form = APDDataReader.readString(buf).trim().substring("%CI".length());
                }
                APDDataReader.readBinaryCustomData(position, buf, form);
            }

            positions.add(position);

        }

        return positions;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.getUnsignedShort(buf.readerIndex()) == 0xfe02) {
            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(buf.retain(), remoteAddress)); // keep-alive message
            }
            return null;
        } else if (buf.getByte(buf.readerIndex()) == '$') {
            return decodeInfo(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII).trim());
        } else if (buf.getByte(buf.readerIndex() + 2) == ',') {
            return decodeText(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII).trim());
        } else {

            String prefix = buf.readCharSequence(2, StandardCharsets.US_ASCII).toString();
            buf.readUnsignedShort(); // checksum
            buf.readUnsignedShort(); // length
            int index = buf.readUnsignedShort();

            long id = buf.readLong();
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(id));
            if (deviceSession == null) {
                return null;
            }

            sendResponse(channel, remoteAddress, id, index);

            if (prefix.equals("@R")) {
                return decodePhoto(deviceSession, buf, id);
            } else {
                return decodeBinary(deviceSession, buf);
            }

        }
    }

}
