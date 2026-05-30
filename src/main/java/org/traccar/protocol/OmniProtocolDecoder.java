package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Command;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Pattern;

public class OmniProtocolDecoder extends BaseProtocolDecoder {

    private String pendingCommand;
    private String vendor = "OM";
    private boolean commandMessageDialect;

    public void setPendingCommand(String pendingCommand) {
        this.pendingCommand = pendingCommand;
    }

    public OmniProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    static ByteBuf encodeFrame(String content) {
        ByteBuf buffer = Unpooled.buffer(content.length() + 2);
        buffer.writeByte(0xff);
        buffer.writeByte(0xff);
        buffer.writeCharSequence(content, StandardCharsets.US_ASCII);
        return buffer;
    }

    public ByteBuf encodeCommand(String uniqueId, String content) {
        String prefix;
        if (commandMessageDialect) {
            String sequence = DateTimeFormatter.ofPattern("yyMMddHHmmss").format(LocalDateTime.now());
            prefix = String.format("*CMDS,%s,%s,%s,", vendor, uniqueId, sequence);
        } else {
            prefix = String.format("*SCOS,%s,%s,", vendor, uniqueId);
        }
        return encodeFrame(prefix + content + "#\n");
    }

    private static final Pattern PATTERN_RMC = new PatternBuilder()
            .text("$")
            .expression("G[PNL]RMC,")
            .number("(dd)(dd)(dd).?d*,")         // time (hhmmss)
            .expression("([AV]),")               // validity
            .number("(dd)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(d{2,3})(dd.d+),")          // longitude
            .expression("([EW]),")
            .number("(d+.?d*)?,")                // speed
            .number("(d+.?d*)?,")                // course
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .any()
            .compile();

    private static final Pattern PATTERN_GGA = new PatternBuilder()
            .text("$")
            .expression("G[PNL]GGA,")
            .number("(dd)(dd)(dd).?d*,")         // time (hhmmss)
            .number("(d+)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(d+)(dd.d+),")              // longitude
            .expression("([EW]),")
            .number("(d+),")                     // fix quality
            .number("(d+),")                     // satellites
            .number("(d+.?d*),")                 // hdop
            .number("(-?d+.?d*),")               // altitude
            .any()
            .compile();

    private static final Pattern PATTERN_GLL = new PatternBuilder()
            .text("$")
            .expression("G[PNL]GLL,")
            .number("(d+)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(d+)(dd.d+),")              // longitude
            .expression("([EW]),")
            .number("(dd)(dd)(dd).?d*,")         // time (hhmmss)
            .expression("([AV]),")               // validity
            .any()
            .compile();

    private static int parseInt(String value) {
        return value != null && !value.isEmpty() ? Integer.parseInt(value) : 0;
    }

    private static double parseDouble(String value) {
        return value != null && !value.isEmpty() ? Double.parseDouble(value) : 0;
    }

    private static double parseCoordinate(String value, String hemisphere) {
        double coordinate = parseDouble(value);
        int degrees = (int) (coordinate / 100);
        double minutes = coordinate - degrees * 100;
        coordinate = degrees + minutes / 60;
        if ("S".equals(hemisphere) || "W".equals(hemisphere)) {
            coordinate = -coordinate;
        }
        return coordinate;
    }

    private static String formatResult(String type, String[] values) {
        return values.length > 0 ? type + "," + String.join(",", values) : type;
    }

    private static int[] parseTime(String value) {
        return new int[] {
                Integer.parseInt(value.substring(0, 2)),
                Integer.parseInt(value.substring(2, 4)),
                Integer.parseInt(value.substring(4, 6))};
    }

    private void sendResponse(
            Channel channel, SocketAddress remoteAddress, String uniqueId, String sequence, String type) {
        if (channel != null) {
            String content;
            if (commandMessageDialect) {
                content = String.format("*CMDS,%s,%s,%s,%s#\n", vendor, uniqueId, sequence, type);
            } else {
                content = String.format("*SCOS,%s,%s,%s#\n", vendor, uniqueId, type);
            }
            channel.writeAndFlush(new NetworkMessage(
                    encodeFrame(content), remoteAddress));
        }
    }

    private void handlePendingCommand(
            Channel channel, SocketAddress remoteAddress, String uniqueId, String[] values) {

        if (channel == null || pendingCommand == null || values.length < 4) {
            return;
        }

        String type = pendingCommand.equals(Command.TYPE_ENGINE_STOP)
                || pendingCommand.equals(Command.TYPE_ALARM_ARM) ? "L1" : "L0";
        channel.writeAndFlush(new NetworkMessage(
                encodeCommand(uniqueId, String.format("%s,%s,%s,%s", type, values[1], values[2], values[3])),
                remoteAddress));
        pendingCommand = null;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        String sentence = ((String) msg).trim();

        if (sentence.isEmpty()) {
            return null;
        }

        while (sentence.startsWith("\u00ff")) {
            sentence = sentence.substring(1);
        }

        if (sentence.startsWith("$")) {
            return decodeNmea(channel, remoteAddress, sentence);
        }

        if (sentence.endsWith("#")) {
            sentence = sentence.substring(0, sentence.length() - 1);
        }

        String[] fields = sentence.split(",", -1);
        if (fields.length < 4
                || (!fields[0].equals("*SCOR") && !fields[0].equals("*HBCR") && !fields[0].equals("*CMDR"))) {
            return null;
        }

        vendor = fields[1];
        commandMessageDialect = fields[0].equals("*CMDR");

        String uniqueId = fields[2];
        int typeIndex = fields[0].equals("*CMDR") ? 4 : 3;
        if (!commandMessageDialect && fields.length > typeIndex + 1 && fields[typeIndex].matches("\\d{12}")) {
            typeIndex += 1;
        }
        if (fields.length <= typeIndex) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, uniqueId);
        if (deviceSession == null) {
            return null;
        }

        String type = fields[typeIndex];
        String[] values = new String[Math.max(0, fields.length - typeIndex - 1)];
        System.arraycopy(fields, typeIndex + 1, values, 0, values.length);

        switch (type) {
            case "D0":
                return decodePosition(deviceSession, values);
            case "R0":
                handlePendingCommand(channel, remoteAddress, uniqueId, values);
                return decodeAttributes(deviceSession, type, values);
            case "L0":
            case "L1":
            case "W0":
            case "E0":
            case "S1":
            case "L5":
                sendResponse(channel, remoteAddress, uniqueId, commandMessageDialect ? fields[3] : null, type);
                return decodeAttributes(deviceSession, type, values);
            default:
                return decodeAttributes(deviceSession, type, values);
        }
    }

    private Position decodePosition(DeviceSession deviceSession, String[] values) {
        if (values.length < 13) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        getLastLocation(position, null);

        String status = values[2];
        position.setValid(status.equals("A"));

        if (status.equals("A") && !values[3].isEmpty() && !values[5].isEmpty()) {
            position.setLatitude(parseCoordinate(values[3], values[4]));
            position.setLongitude(parseCoordinate(values[5], values[6]));
        }

        if (!values[1].isEmpty() && !values[9].isEmpty()) {
            int[] time = parseTime(values[1]);
            int[] date = parseTime(values[9]);
            position.setTime(new DateBuilder()
                    .setDateReverse(date[0], date[1], date[2])
                    .setTime(time[0], time[1], time[2])
                    .getDate());
        }

        position.set(Position.KEY_SATELLITES, parseInt(values[7]));
        position.set(Position.KEY_HDOP, parseDouble(values[8]));
        position.setAltitude(parseDouble(values[10]));
        position.set(Position.KEY_STATUS, parseInt(values[0]));
        position.set("mode", values[12]);

        return position;
    }

    private Position decodeAttributes(DeviceSession deviceSession, String type, String[] values) {
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        getLastLocation(position, new Date());
        position.setTime(new Date());
        position.set("omni", formatResult(type, values));

        switch (type) {
            case "Q0":
                if (values.length >= 3) {
                    position.set(Position.KEY_BATTERY, parseInt(values[0]) * 0.01);
                    position.set(Position.KEY_BATTERY_LEVEL, parseInt(values[1]));
                    position.set(Position.KEY_RSSI, parseInt(values[2]));
                }
                break;
            case "H0":
                if (values.length >= 5) {
                    position.set(Position.KEY_BLOCKED, parseInt(values[0]) > 0);
                    position.set(Position.KEY_BATTERY, parseInt(values[1]) * 0.01);
                    position.set(Position.KEY_RSSI, parseInt(values[2]));
                    position.set(Position.KEY_BATTERY_LEVEL, parseInt(values[3]));
                    position.set(Position.KEY_CHARGE, parseInt(values[4]) > 0);
                }
                break;
            case "S6":
                if (values.length >= 8) {
                    position.set(Position.KEY_BATTERY_LEVEL, parseInt(values[0]));
                    position.set(Position.KEY_STATUS, parseInt(values[1]));
                    position.setSpeed(parseInt(values[2]) * 0.1);
                    position.set(Position.KEY_CHARGE, parseInt(values[3]) > 0);
                    position.set("battery1", parseInt(values[4]) * 0.1);
                    position.set("battery2", parseInt(values[5]) * 0.1);
                    position.set(Position.KEY_BLOCKED, parseInt(values[6]) > 0);
                    position.set(Position.KEY_RSSI, parseInt(values[7]));
                }
                break;
            case "W0":
                if (values.length >= 1) {
                    position.set("alarmCode", parseInt(values[0]));
                    switch (parseInt(values[0])) {
                        case 1:
                            position.set(Position.KEY_ALARM, Position.ALARM_MOVEMENT);
                            break;
                        case 2:
                            position.set(Position.KEY_ALARM, Position.ALARM_FALL_DOWN);
                            break;
                        case 4:
                            position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                            break;
                        default:
                            position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
                            break;
                    }
                }
                break;
            case "E0":
                if (values.length >= 1) {
                    position.set(Position.KEY_ALARM, Position.ALARM_FAULT);
                    position.set("error", parseInt(values[0]));
                }
                break;
            case "S1":
                if (values.length >= 1) {
                    position.set(Position.KEY_EVENT, parseInt(values[0]));
                }
                break;
            case "K0":
                // BLE 8-byte communication key reported by the lock (Omni K0
                // response, e.g. "OmniW4GX"). Exposed for the app's BLE unlock.
                if (values.length >= 1 && !values[0].isEmpty()) {
                    position.set("omniBleKey", values[0]);
                }
                position.set(Position.KEY_RESULT, formatResult(type, values));
                break;
            case "D1":
            case "R0":
            case "L0":
            case "L1":
            case "S2":
            case "S4":
            case "S5":
            case "S7":
            case "V0":
            case "V1":
            case "G0":
            case "I0":
            case "M0":
            case "L5":
            case "Z0":
            case "Z1":
            case "D2":
            case "U0":
            case "U1":
            case "U2":
            case "U5":
            case "U6":
            case "B0":
            case "C0":
                position.set(Position.KEY_RESULT, formatResult(type, values));
                break;
            default:
                return null;
        }

        return !position.getAttributes().isEmpty() ? position : null;
    }

    private Position decodeNmea(Channel channel, SocketAddress remoteAddress, String sentence) {
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        if (sentence.contains("RMC,")) {
            return decodeRmc(deviceSession, sentence);
        } else if (sentence.contains("GGA,")) {
            return decodeGga(deviceSession, sentence);
        } else if (sentence.contains("GLL,")) {
            return decodeGll(deviceSession, sentence);
        }

        return null;
    }

    private Position decodeRmc(DeviceSession deviceSession, String sentence) {
        Parser parser = new Parser(PATTERN_RMC, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        return position;
    }

    private Position decodeGga(DeviceSession deviceSession, String sentence) {
        Parser parser = new Parser(PATTERN_GGA, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setCurrentDate()
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setValid(parser.nextInt() > 0);
        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_HDOP, parser.nextDouble());
        position.setAltitude(parser.nextDouble());

        return position;
    }

    private Position decodeGll(DeviceSession deviceSession, String sentence) {
        Parser parser = new Parser(PATTERN_GLL, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        DateBuilder dateBuilder = new DateBuilder()
                .setCurrentDate()
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());
        position.setValid(parser.next().equals("A"));

        return position;
    }
}
