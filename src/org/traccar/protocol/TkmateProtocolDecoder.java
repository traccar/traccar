package org.traccar.protocol;

import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.TimeZone;
import java.util.regex.Pattern;


public class TkmateProtocolDecoder extends BaseProtocolDecoder {

    private final TimeZone timeZone = TimeZone.getTimeZone("UTC");

    public TkmateProtocolDecoder(TkmateProtocol protocol) {
        super(protocol);
        timeZone.setRawOffset(Context.getConfig().getInteger(getProtocolName() + ".timezone") * 1000);
    }

    private static final Pattern PATTERN_SRT = new PatternBuilder()
            .text("^TMSRT|")
            .expression("([^ ]+)|")              // uid
            .number("(d+.d+)|")                  // latitude
            .number("(d+.d+)|")                  // longitude
            .number("(dd)(dd)(dd)|")              // time
            .number("(dd)(dd)(dd)|")              // date
            .number("(d+.d+)|")                  // software ver
            .number("(d+.d+)|")                  // Hardware ver
            .any()
            .compile();


    private static final Pattern PATTERN_PER = new PatternBuilder()
            .text("^TMPER|")
            .expression("([^ ]+)|")              // uid
            .number("(d+)|")                     // seq
            .number("(d+.d+)|")                  // latitude
            .number("(d+.d+)|")                  // longitude
            .number("(dd)(dd)(dd)|")              // time
            .number("(dd)(dd)(dd)|")              // date
            .number("(d+.d+)|")                  // speed
            .number("(d+.d+)|")                  // heading
            .number("(d+)|")                     // ignition
            .number("(d+)|")                     // dop1
            .number("(d+)|")                     // dop2
            .number("(d+.d+)|")                  // analog
            .number("(d+.d+)|")                  // internal battery
            .number("(d+.d+)|")                  // vehicle battery
            .number("(d+.d+)|")                  // gps odometer
            .number("(d+.d+)|")                  // pulse odometer
            .number("(d+)|")                     // main power status
            .number("(d+)|")                     // gps data validity
            .number("(d+)|")                     // live or cache
            .any()
            .compile();


    private static final Pattern PATTERN_ALT = new PatternBuilder()
            .text("^TMALT|")
            .expression("([^ ]+)|")              // uid
            .number("(d+)|")                     // seq
            .number("(d+)|")                     // Alert type
            .number("(d+)|")                     // Alert status
            .number("(d+.d+)|")                  // latitude
            .number("(d+.d+)|")                  // longitude
            .number("(dd)(dd)(dd)|")              // time
            .number("(dd)(dd)(dd)|")              // date
            .number("(d+.d+)|")                  // speed
            .number("(d+.d+)|")                     // heading
            .any()
            .compile();


    private String decodeAlarm(int value) {
        switch (value) {
            case 1:
                return Position.ALARM_SOS;
            case 3:
                return Position.ALARM_GEOFENCE;
            case 4:
                return Position.ALARM_POWER_CUT;
            default:
                return null;
        }
    }

    private Object decodeSrt(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_SRT, sentence);
        if (!parser.matches()) {
            return null;
        }
        Position position = new Position();
        position.setProtocol(getProtocolName());
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        DateBuilder dateBuilder = new DateBuilder(timeZone)
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());
        position.set(Position.KEY_VERSION, parser.next());
        parser.next(); //hardware version
        return position;
    }


    private Object decodeAlt(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_ALT, sentence);
        if (!parser.matches()) {
            return null;
        }
        Position position = new Position();
        position.setProtocol(getProtocolName());
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());
        parser.next();  // seq
        int alarm = parser.nextInt();
        position.set(Position.KEY_ALARM, decodeAlarm(alarm));
        parser.next();  //alert status or data
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        DateBuilder dateBuilder = new DateBuilder(timeZone)
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());
        return position;
    }


    protected Object decodePer(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        Parser parser = new Parser(PATTERN_PER, (String) msg);
        if (!parser.matches()) {
            return null;
        }
        Position position = new Position();
        position.setProtocol(getProtocolName());
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());
        parser.next();  //seq
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        DateBuilder dateBuilder = new DateBuilder(timeZone)
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());
        position.set(Position.KEY_IGNITION, parser.nextInt() == 1);
        parser.next(); //dop1
        parser.next(); //dop2
        parser.next(); //analog input
        position.set(Position.KEY_BATTERY, parser.nextDouble()); //device battery voltage
        parser.next();  //vehicle internal voltage
        position.set(Position.KEY_ODOMETER, parser.nextDouble()); //GPS odometer
        parser.next();  //pulse odometer
        position.set(Position.KEY_STATUS, parser.nextInt());
        position.setValid(parser.nextInt() != 0);
        position.set(Position.KEY_ARCHIVE, parser.nextInt() == 1);
        return position;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        String sentence = (String) msg;
        int typeIndex = sentence.indexOf("^TM");
        if (typeIndex < 0) {
            return null;
        }

        Object result;
        String type = sentence.substring(typeIndex + 3, typeIndex + 6);
        switch (type) {
            case "ALT":
                result = decodeAlt(channel, remoteAddress, sentence);
                break;
            case "SRT":
                result = decodeSrt(channel, remoteAddress, sentence);
                break;
            case "PER":
                result = decodePer(channel, remoteAddress, sentence);
                break;
            default:
                return null;
        }
        return result;

    }
}

