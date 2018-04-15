/*
 * Copyright 2017 Jan-Piet Mens (jpmens@gmail.com)
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

 /* TODO
 * Position.outdated
 * Position.network
 *
 * public static final String KEY_INDEX = "index";
 * public static final String KEY_HDOP = "hdop";
 * public static final String KEY_VDOP = "vdop";
 * public static final String KEY_PDOP = "pdop";
 * public static final String KEY_SATELLITES = "sat"; // in use
 * public static final String KEY_SATELLITES_VISIBLE = "satVisible";
 * public static final String KEY_RSSI = "rssi";
 * public static final String KEY_GPS = "gps";
 * public static final String KEY_ROAMING = "roaming";
 * public static final String KEY_STATUS = "status";
 * public static final String KEY_ODOMETER_SERVICE = "serviceOdometer"; // meters
 * public static final String KEY_ODOMETER_TRIP = "tripOdometer"; // meters
 * public static final String KEY_HOURS = "hours";
 * public static final String KEY_STEPS = "steps";
 * public static final String KEY_INPUT = "input";
 * public static final String KEY_OUTPUT = "output";

 * public static final String KEY_FUEL_LEVEL = "fuel"; // liters
 * public static final String KEY_FUEL_CONSUMPTION = "fuelConsumption"; // liters/hour

 * public static final String KEY_VERSION_FW = "versionFw";
 * public static final String KEY_VERSION_HW = "versionHw";
 * public static final String KEY_TYPE = "type";
 * public static final String KEY_FLAGS = "flags";
 * public static final String KEY_CHARGE = "charge";
 * public static final String KEY_IP = "ip";
 * public static final String KEY_ARCHIVE = "archive";
 * public static final String KEY_DISTANCE = "distance"; // meters
 * public static final String KEY_TOTAL_DISTANCE = "totalDistance"; // meters
 * public static final String KEY_APPROXIMATE = "approximate";
 * public static final String KEY_THROTTLE = "throttle";
 *
 * // Start with 1 not 0
 * public static final String PREFIX_TEMP = "temp";
 * public static final String PREFIX_ADC = "adc";
 * public static final String PREFIX_IO = "io";
 * public static final String PREFIX_COUNT = "count";
 * public static final String PREFIX_IN = "in";
 * public static final String PREFIX_OUT = "out";
 *
 * public static final String ALARM_GENERAL = "general";
 * public static final String ALARM_SOS = "sos";
 * public static final String ALARM_VIBRATION = "vibration";
 * public static final String ALARM_MOVEMENT = "movement";
 * public static final String ALARM_LOW_POWER = "lowPower";
 * public static final String ALARM_LOW_BATTERY = "lowBattery";
 * public static final String ALARM_FAULT = "fault";
 * public static final String ALARM_POWER_OFF = "powerOff";
 * public static final String ALARM_POWER_ON = "powerOn";
 * public static final String ALARM_DOOR = "door";
 * public static final String ALARM_GEOFENCE = "geofence";
 * public static final String ALARM_GEOFENCE_ENTER = "geofenceEnter";
 * public static final String ALARM_GEOFENCE_EXIT = "geofenceExit";
 * public static final String ALARM_GPS_ANTENNA_CUT = "gpsAntennaCut";
 * public static final String ALARM_ACCIDENT = "accident";
 * public static final String ALARM_ACCELERATION = "hardAcceleration";
 * public static final String ALARM_BRAKING = "hardBraking";
 * public static final String ALARM_CORNERING = "hardCornering";
 * public static final String ALARM_FATIGUE_DRIVING = "fatigueDriving";
 * public static final String ALARM_JAMMING = "jamming";
 * public static final String ALARM_TEMPERATURE = "temperature";
 * public static final String ALARM_SHOCK = "shock";
 * public static final String ALARM_BONNET = "bonnet";
 * public static final String ALARM_FOOT_BRAKE = "footBrake";
 * public static final String ALARM_OIL_LEAK = "oilLeak";
 * public static final String ALARM_TAMPERING = "tampering";
 * public static final String ALARM_REMOVING = "removing";
 *
 *
 * Network.radiotype // String e.g. "gsm"
 * Network.carrier // String e.g. "gsm"
 * Network.homeMobileCountryCode // Integer MCC
 * Network.homeMobileNetworkCode // Integer MNC
 * Network.considerIp // Boolean
 * Network.wifiAccessPoints // Collection
 * Network.cellTowers // Collection
 *
 * WifiAccesPoint.macAddress // String
 * WifiAccesPoint.signalStrength // Integer
 * WifiAccesPoint.channel // Integer
 *
 * CellTowers.radioType // String e.g. "gsm"
 * CellTowers.cellId // Long CID
 * CellTowers.locationAreaCode // Integer LAC
 * CellTowers.mobileCountryCode // Integer MCC
 * CellTowers.mobileNetworkCode // Integer MNC
 * CellTowers.signalStrength // Integer
 */
package org.traccar.protocol;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.model.Event;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class OwnTracksProtocolDecoder extends BaseHttpProtocolDecoder {

    public OwnTracksProtocolDecoder(OwnTracksProtocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        HttpRequest request = (HttpRequest) msg;
        JsonObject root = Json.createReader(
                new StringReader(request.getContent().toString(StandardCharsets.US_ASCII))).readObject();

        if (!root.containsKey("_type") || !root.getString("_type").equals("location")) {
            sendResponse(channel, HttpResponseStatus.OK);
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        //position.set(Position.KEY_ORIGINAL, request.getContent().toString(StandardCharsets.US_ASCII));

        position.setValid(true);

        position.setLatitude(root.getJsonNumber("lat").doubleValue());
        position.setLongitude(root.getJsonNumber("lon").doubleValue());

        if (root.containsKey("vel")) {
            position.setSpeed(UnitsConverter.knotsFromKph(root.getInt("vel")));
        }
        if (root.containsKey("alt")) {
            position.setAltitude(root.getInt("alt"));
        }
        if (root.containsKey("cog")) {
            position.setCourse(root.getInt("cog"));
        }
        if (root.containsKey("acc")) {
            position.setAccuracy(root.getInt("acc"));
        }
        if (root.containsKey("t")) {
            String t = root.getString("t");
            position.set("t", t);

            if (t == "9") {
                position.set(Position.KEY_EVENT, Event.TYPE_ALARM);
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
            } else if (t == "1") {
                position.set(Position.KEY_EVENT, Event.TYPE_ALARM);
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_ON);
            } else if (t == "i") {
                position.set(Position.KEY_EVENT, Event.TYPE_IGNITION_ON);
            } else if (t == "I") {
                position.set(Position.KEY_EVENT, Event.TYPE_IGNITION_OFF);
            } else if (t == "e") {
                position.set(Position.KEY_EVENT, Event.TYPE_ALARM);
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_RESTORED);
            } else if (t == "E") {
                position.set(Position.KEY_EVENT, Event.TYPE_ALARM);
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
            } else if (t == "e" || t == "k") {
                position.set(Position.KEY_EVENT, Event.TYPE_ALARM);
                position.set(Position.KEY_ALARM, Position.ALARM_PARKING);
            } else if (t == "v") {
                position.set(Position.KEY_EVENT, Event.TYPE_ALARM);
                position.set(Position.KEY_ALARM, Position.ALARM_MOVEMENT);
            } else if (t == "!") {
                position.set(Position.KEY_EVENT, Event.TYPE_ALARM);
                position.set(Position.KEY_ALARM, Position.ALARM_TOW);
            }
            /* these triggers are not handled as alarms
                * `b` beacon region enter/leave event
                * `c` circular region enter/leave event
                * `f` First publish after reboot
                * `l` Last known position when device lost GPS fix
                * `L` Last known position before gracefull shutdown
                * `m` Manually requested locations (e.g. by publishing to `/cmd`)
                * `p` ping issued randomly by background task
                * `r` response to a reportLocation cmd message
                * `t` timer based publish in move move
                * `t` Time for location published because device is moving.
                * `T` Time for location published because of time passed while device is stationary (`maxInterval`)
                * `u` manual publish requested by the user

                * `o` Corner
                * `M` Mileage
                * `2` Battery stop charging
                * `3` Battery start charging
            */
        }
        if (root.containsKey("batt")) {
            position.set(Position.KEY_BATTERY_LEVEL, root.getInt("batt"));
        }
        if (root.containsKey("uext")) {
            position.set(Position.KEY_POWER, root.getJsonNumber("uext").doubleValue());
        }
        if (root.containsKey("ubatt")) {
            position.set(Position.KEY_BATTERY, root.getJsonNumber("ubatt").doubleValue());
        }
        if (root.containsKey("vin")) {
            position.set(Position.KEY_VIN, root.getString("vin"));
        }
        if (root.containsKey("name")) {
            position.set(Position.KEY_VIN, root.getString("name"));
        }
        if (root.containsKey("rpm")) {
            position.set(Position.KEY_RPM, root.getInt("rpm"));
        }
        if (root.containsKey("ign")) {
            position.set(Position.KEY_IGNITION, root.getBoolean("ign"));
        }
        if (root.containsKey("motion")) {
            position.set(Position.KEY_MOTION, root.getBoolean("motion"));
        }


        position.setTime(new Date(root.getJsonNumber("tst").longValue() * 1000));
        if (root.containsKey("sent")) {
            position.setDeviceTime(new Date(root.getJsonNumber("sent").longValue() * 1000));
        }

        String uniqueId;

        if (root.containsKey("topic")) {
            uniqueId = root.getString("topic");
            if (root.containsKey("tid")) {
                position.set("tid", root.getString("tid"));
            }
        } else {
            uniqueId = root.getString("tid");
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, uniqueId);
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        sendResponse(channel, HttpResponseStatus.OK);
        return position;
    }
}
