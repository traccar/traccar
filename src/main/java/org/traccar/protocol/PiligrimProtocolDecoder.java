/*
 * Copyright 2014 - 2020 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import net.fortuna.ical4j.model.DateTime;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.NMEA;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class PiligrimProtocolDecoder extends BaseHttpProtocolDecoder {

    public PiligrimProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, String message) {
        sendResponse(channel, HttpResponseStatus.OK, Unpooled.copiedBuffer(message, StandardCharsets.US_ASCII));
    }

    public static final int MSG_GPS = 0xF1;
    public static final int MSG_GPS_SENSORS = 0xF2;
    public static final int MSG_EVENTS = 0xF3;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        String uri = request.uri();

        if (uri.startsWith("/config")) {

            sendResponse(channel, "CONFIG: OK");

        } else if (uri.startsWith("/addlog")) {

            sendResponse(channel, "ADDLOG: OK");

        } else if (uri.startsWith("/inform")) {

            sendResponse(channel, "INFORM: OK");

        } else if (uri.startsWith("/bingps")) {

            sendResponse(channel, "BINGPS: OK");

            QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
            DeviceSession deviceSession = getDeviceSession(
                    channel, remoteAddress, decoder.parameters().get("imei").get(0));
            if (deviceSession == null) {
                return null;
            }

            List<Position> positions = new LinkedList<>();
            ByteBuf buf = request.content();

            while (buf.readableBytes() > 2) {

                buf.readUnsignedByte(); // header
                int type = buf.readUnsignedByte();
                buf.readUnsignedByte(); // length

                if (type == MSG_GPS || type == MSG_GPS_SENSORS) {

                    Position position = new Position(getProtocolName());
                    position.setDeviceId(deviceSession.getDeviceId());

                    DateBuilder dateBuilder = new DateBuilder()
                            .setDay(buf.readUnsignedByte())
                            .setMonth(buf.getByte(buf.readerIndex()) & 0x0f)
                            .setYear(2010 + (buf.readUnsignedByte() >> 4))
                            .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
                    position.setTime(dateBuilder.getDate());

                    double latitude = buf.readUnsignedByte();
                    latitude += buf.readUnsignedByte() / 60.0;
                    latitude += buf.readUnsignedByte() / 6000.0;
                    latitude += buf.readUnsignedByte() / 600000.0;

                    double longitude = buf.readUnsignedByte();
                    longitude += buf.readUnsignedByte() / 60.0;
                    longitude += buf.readUnsignedByte() / 6000.0;
                    longitude += buf.readUnsignedByte() / 600000.0;

                    int flags = buf.readUnsignedByte();
                    if (BitUtil.check(flags, 0)) {
                        latitude = -latitude;
                    }
                    if (BitUtil.check(flags, 1)) {
                        longitude = -longitude;
                    }
                    position.setLatitude(latitude);
                    position.setLongitude(longitude);

                    int satellites = buf.readUnsignedByte();
                    position.set(Position.KEY_SATELLITES, satellites);
                    position.setValid(satellites >= 3);

                    position.setSpeed(buf.readUnsignedByte());

                    double course = buf.readUnsignedByte() << 1;
                    course += (flags >> 2) & 1;
                    course += buf.readUnsignedByte() / 100.0;
                    position.setCourse(course);

                    if (type == MSG_GPS_SENSORS) {
                        double power = buf.readUnsignedByte();
                        power += buf.readUnsignedByte() << 8;
                        position.set(Position.KEY_POWER, power * 0.01);

                        double battery = buf.readUnsignedByte();
                        battery += buf.readUnsignedByte() << 8;
                        position.set(Position.KEY_BATTERY, battery * 0.01);

                        buf.skipBytes(6);
                    }

                    positions.add(position);

                } else if (type == MSG_EVENTS) {

                    buf.skipBytes(13);
                }
            }

            return positions;
        } else if (uri.startsWith("/push.do")) {
            sendResponse(channel, "PUSH.DO: OK");

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, "123456");
            if (deviceSession == null) {
                return null;
            }

            /* Getting payload */
            ByteBuf content_stream = request.content();
            byte[] payload_bytes = new byte[Integer.parseInt(request.headers().get("Content-Length"))];
            content_stream.readBytes(payload_bytes);
            String payload = new String(payload_bytes);

            /* Payload structure:
             * &phone&message
             */
            String[] payload_parts = payload.split("&");
            /* System.out.println("Payload parts: " + Arrays.toString(payload_parts)); */
            String phone_number = payload_parts[1].substring(15);
            String message = payload_parts[2].substring(8);
            /* System.out.println("Phone number: " + phone_number); */
            /* System.out.println("Message: " + message); */

            if (message.startsWith("$GPRMC")) {
                /* Supported message structure:
                 * GPS NMEA Command; GSM info; Unknown; Battery voltage?
                 * Example: $GPRMC,180752.000,A,5314.0857,N,03421.8173,E,0.00,104.74,220722,,,A,V* 29,05; GSM: 250-01 0b54-0519,1c30,3e96,3ebe,412e 25;  S; Batt: 405,M
                 */
                System.out.println("Supported message");

                String[] message_parts = message.split(";");
                /* System.out.println("Message parts: " + Arrays.toString(message_parts)); */

                /* Parsing GPS */
                String unprocessed_gps_command = message_parts[0];

                /* Getting rid of checksum */
                String gps_command = unprocessed_gps_command.replaceFirst("A,V[*].*", "");
                /* System.out.println("GPS command: " + gps_command); */

                NMEA gps_parser = new NMEA();

                NMEA.GPSPosition gps_position = gps_parser.parse(gps_command);

                /* System.out.println("Time: " + gps_position.time); */
                /* System.out.println("Coordinates: " + gps_position.lat + " " + gps_position.lon); */
                /* System.out.println("Speed over ground: " + gps_position.velocity + " knots"); */

                /* Parsing other fields */
                /* String gsm_info = message_parts[1]; */
                /* String unknown = message_parts[2]; */
                String battery_info = message_parts[3].substring(7).substring(0, 3);
                /* System.out.println("Battery: " + battery_info); */

                /* Constructing response */
                Position position = new Position(getProtocolName());

                position.setDeviceId(deviceSession.getDeviceId());
                position.setValid(true);
                position.setLatitude(gps_position.lat);
                position.setLongitude(gps_position.lon);
                position.setTime(new Date(System.currentTimeMillis()));
                position.setSpeed(gps_position.velocity);
                position.setCourse(gps_position.dir);
                position.setAccuracy(gps_position.quality);
                position.set(Position.KEY_BATTERY, Integer.parseInt(battery_info) / 100);

                System.out.println("Supported message finish");

                return position;
            } else {
                System.out.println("Unsupported message");
            }
        }

        return null;
    }

}
