package org.traccar.protocol;

import java.net.SocketAddress;
import java.util.regex.Pattern;

import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.Parser.CoordinateFormat;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

public class LdplProtocolDecoder extends BaseProtocolDecoder {

    public LdplProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
                .text("*ID")                         // start of frame
                .number("(d+),")                     // command_code
                .number("(d+),")                     // imei
                .number("(dd)(dd)(dd),")             // current_date
                .number("(dd)(dd)(dd),")             // current_time
                .expression("([A|V]),")              // gps_fix
                .number("(dd)(dd).?(d+),([NS]),")    // latitude
                .number("(ddd)(dd).?(d+),([EW]),")   // longitude
                .number("(d{1,3}.dd),")              // speed
                .number("(d{1,3}.dd),")              // course
                .number("(d{1,2}),")                 // sats
                .number("(d{1,3}),")                 // gsm_signal_strength
                .expression("([A|N|S]),")            // vehicle_status
                .expression("([0|1]),")              // main_power_status
                .number("(d.dd),")                   // internal_battery_voltage
                .expression("([0|1]),")              // sos_alert
                .expression("([0|1]),")              // body_tamper
                .expression("([0|1])([0|1]),")       // ac_status + ign_status
                .expression("([0|1|2]),")            // output1_status
                .number("(d{1,3}),")                 // adc1
                .number("(d{1,3}),")                 // adc2
                .expression("([0-9A-Z]{3}),")        // software_version
                .expression("([L|R]),")              // message_type
                .expression("([0-9A-Z]{4})#")        // crc
                .compile();

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.set(Position.KEY_TYPE, parser.nextInt());

        if (!identify(parser.next(), channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
        .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt())
        .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        if ("A".equals(parser.next())) {
            position.setValid(true);
        } else {
            position.setValid(false);
        }
        
        position.setLatitude(parser.nextCoordinate(CoordinateFormat.DEG_MIN_MIN_HEM));
        position.setLongitude(parser.nextCoordinate(CoordinateFormat.DEG_MIN_MIN_HEM));
        
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());
        
        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_GSM, parser.nextInt());
        String vehicleStatus = parser.next();
        position.set(Position.KEY_POWER, parser.nextInt());
        position.set(Position.KEY_BATTERY, parser.nextDouble());
        position.set(Position.KEY_ALARM, parser.nextInt());
        Integer bodyTamper = parser.nextInt();
        Integer acStatus = parser.nextInt();
        position.set(Position.KEY_IGNITION, parser.nextInt());
        position.set(Position.KEY_OUTPUT, parser.nextInt());
        position.set(Position.PREFIX_ADC + 1, parser.nextInt());
        position.set(Position.PREFIX_ADC + 2, parser.nextInt());
        position.set(Position.KEY_VERSION, parser.next());
        
        if ("R".equals(parser.next())) {
            position.set(Position.KEY_ARCHIVE, true);
        } else {
            position.set(Position.KEY_ARCHIVE, false);
        }
        
        String checksum = parser.next();

        return position;
    }

}
