/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.Channel;
import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.DateUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class HowenProtocolDecoder extends BaseProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HowenProtocolDecoder.class);

    private static final int HEADER_FLAG = 0x48;
    private static final int PROTOCOL_VERSION = 0x01;

    private static final int MSG_HEARTBEAT = 0x0001;
    private static final int MSG_REGISTRATION = 0x1001;
    private static final int MSG_STATUS = 0x1041;
    private static final int MSG_ALARM = 0x1051;

    private static final int MSG_REGISTRATION_RESPONSE = 0x4001;
    private static final int MSG_STATUS_SUBSCRIPTION = 0x4040;
    private static final int MSG_STATUS_RESPONSE = 0x4041;
    private static final int MSG_ALARM_SUBSCRIPTION = 0x4050;
    private static final int MSG_ALARM_RESPONSE = 0x4051;

    private static final String ATTR_SUBSCRIBED = "howenSubscribed";
    private static final String ATTR_SESSION = "howenSession";
    private static final long TIME_OFFSET_MILLIS = TimeUnit.HOURS.toMillis(7);

    public HowenProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.readableBytes() < 8) {
            return null;
        }

        int flag = buf.readUnsignedByte();
        if (flag != HEADER_FLAG) {
            return null;
        }

        buf.readUnsignedByte(); // version
        int type = buf.readUnsignedShortLE();
        int length = buf.readIntLE();
        ByteBuf payload = buf.readSlice(length);

        return switch (type) {
            case MSG_HEARTBEAT -> decodeHeartbeat(channel, remoteAddress);
            case MSG_REGISTRATION -> decodeRegistration(channel, remoteAddress, payload);
            case MSG_STATUS -> decodeStatus(channel, remoteAddress, payload);
            case MSG_ALARM -> decodeAlarm(channel, remoteAddress, payload);
            default -> {
                LOGGER.info("howen message type not handled: 0x{}", Integer.toHexString(type));
                yield null;
            }
        };
    }

    private Object decodeHeartbeat(Channel channel, SocketAddress remoteAddress) {
        LOGGER.debug("Received heartbeat");
        sendResponse(channel, remoteAddress, MSG_HEARTBEAT, null);
        return null;
    }

    private Object decodeRegistration(Channel channel, SocketAddress remoteAddress, ByteBuf payload) {
        LOGGER.debug("Received registration request");
        String content = readString(payload);
        if (content.isEmpty()) {
            return null;
        }

        JsonObject json = Json.createReader(new StringReader(content)).readObject();

        String uniqueId = json.getString("dn", null);
        if (uniqueId == null) {
            uniqueId = json.getString("guid", null);
        }
        if (uniqueId == null) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, uniqueId);
        if (deviceSession == null) {
            return null;
        }

        String session = json.getString("ss", "");
        if (!session.isEmpty()) {
            deviceSession.set(ATTR_SESSION, session);
        }

        sendJsonResponse(channel, remoteAddress, MSG_REGISTRATION_RESPONSE,
                Json.createObjectBuilder().add("err", "0").add("ss", session).build());

        requestSubscriptions(channel, remoteAddress, deviceSession, session);

        return null;
    }

    private void requestSubscriptions(
            Channel channel, SocketAddress remoteAddress, DeviceSession deviceSession, String session) {

        if (channel == null || deviceSession == null || deviceSession.contains(ATTR_SUBSCRIBED)) {
            return;
        }

        String effectiveSession = session;
        if (effectiveSession == null || effectiveSession.isEmpty()) {
            effectiveSession = deviceSession.get(ATTR_SESSION);
        }

        JsonObjectBuilder statusJson = Json.createObjectBuilder();
        if (effectiveSession != null && !effectiveSession.isEmpty()) {
            statusJson.add("ss", effectiveSession);
        }
        statusJson.add("ct", 15);
        JsonObject statusPayload = statusJson.build();

        JsonObjectBuilder alarmJson = Json.createObjectBuilder();
        if (effectiveSession != null && !effectiveSession.isEmpty()) {
            alarmJson.add("ss", effectiveSession);
        }
        alarmJson.add("ct", 45);
        JsonObject alarmPayload = alarmJson.build();

        LOGGER.info("howen subscription request: device={}, session={}, statusType=0x4040(ct={}), "
                + "alarmType=0x4050(ct={})",
                deviceSession.getDeviceId(), effectiveSession,
                statusPayload.getInt("ct", 0), alarmPayload.getInt("ct", 0));

        sendJsonResponse(channel, remoteAddress, MSG_STATUS_SUBSCRIPTION, statusPayload);
        sendJsonResponse(channel, remoteAddress, MSG_ALARM_SUBSCRIPTION, alarmPayload);

        deviceSession.set(ATTR_SUBSCRIBED, true);
    }

    private Object decodeStatus(Channel channel, SocketAddress remoteAddress, ByteBuf payload) {
        LOGGER.debug("Received status message");
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        int sessionLength = payload.readUnsignedByte();
        String session = "";
        if (sessionLength > 0) {
            session = readString(payload, sessionLength);
            deviceSession.set(ATTR_SESSION, session);
        }

        requestSubscriptions(channel, remoteAddress, deviceSession, session);

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        if (!session.isEmpty()) {
            position.set("session", session);
        }

        decodeStatusData(position, payload);

        sendResponse(channel, remoteAddress, MSG_STATUS_RESPONSE, null);

        return position;
    }

    private Object decodeAlarm(Channel channel, SocketAddress remoteAddress, ByteBuf payload) {
        LOGGER.debug("Received alarm message");
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        int sessionLength = payload.readUnsignedByte();
        String session = "";
        if (sessionLength > 0) {
            session = readString(payload, sessionLength);
            deviceSession.set(ATTR_SESSION, session);
        }

        while (payload.isReadable() && payload.getUnsignedByte(payload.readerIndex()) == 0) {
            payload.readUnsignedByte();
        }

        if (payload.readableBytes() < 4) {
            return null;
        }

        int contentLength = payload.readIntLE();

        if (contentLength < 0 || payload.readableBytes() < contentLength) {
            return null;
        }

        String content = readString(payload, contentLength);

        JsonObject json = Json.createReader(new StringReader(content)).readObject();

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        if (!session.isEmpty()) {
            position.set("session", session);
        }

        String deviceTime = json.getString("dtu", null);
        if (deviceTime != null && !deviceTime.isEmpty()) {
            Date parsedTime = null;
            try {
                parsedTime = DateUtil.parseDate(deviceTime);
            } catch (RuntimeException e) {
                try {
                    parsedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(deviceTime);
                } catch (ParseException ignored) {
                    parsedTime = null;
                }
            }
            if (parsedTime != null) {
                Date utcTime = applyTimeOffset(parsedTime);
                position.setDeviceTime(utcTime);
                position.setTime(utcTime);
            }
        }

        String eventCode = json.getString("ec", null);
        if (eventCode != null) {
            try {
                position.set(Position.KEY_EVENT, Integer.parseInt(eventCode));
            } catch (NumberFormatException e) {
                position.set(Position.KEY_EVENT, eventCode);
            }
        }

        if (json.containsKey("det")) {
            position.set("alarmDetail", json.get("det").toString());
        }

        // Decode event-specific alarms based on event code
        decodeEventAlarm(position, eventCode, json);

        decodeStatusData(position, payload);
        applyJsonDerivedValues(position, json);

        sendResponse(channel, remoteAddress, MSG_ALARM_RESPONSE, null);

        return position;
    }

    private void decodeEventAlarm(Position position, String eventCode, JsonObject json) {
        if (eventCode == null || eventCode.isEmpty()) {
            position.addAlarm(Position.ALARM_GENERAL);
            return;
        }

        switch (eventCode) {
            // Phase 1: Critical Safety Alarms
            case "5":
                // Emergency alarm / SOS
                position.addAlarm(Position.ALARM_SOS);
                break;

            case "7":
                // Over speed alarm
                position.addAlarm(Position.ALARM_OVERSPEED);
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("spd")) {
                        try {
                            double speed = detail.getJsonNumber("spd").doubleValue();
                            position.set("alarmSpeed", speed);
                        } catch (Exception ignored) {
                        }
                    }
                    if (detail.containsKey("lmt")) {
                        try {
                            double limit = detail.getJsonNumber("lmt").doubleValue();
                            position.set("speedLimit", limit);
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "17":
                // Overtime driving (fatigue)
                position.addAlarm(Position.ALARM_FATIGUE_DRIVING);
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("dt")) {
                        try {
                            int duration = detail.getInt("dt");
                            position.set("drivingTime", duration);
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "24":
                // Harsh acceleration
                position.addAlarm(Position.ALARM_ACCELERATION);
                break;

            case "25":
                // Harsh braking
                position.addAlarm(Position.ALARM_BRAKING);
                break;

            case "37":
                // Towing
                position.addAlarm(Position.ALARM_TOW);
                break;

            case "48":
                // Excessive overspeed
                position.addAlarm(Position.ALARM_OVERSPEED);
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("spd")) {
                        try {
                            double speed = detail.getJsonNumber("spd").doubleValue();
                            position.set("alarmSpeed", speed);
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "61":
                // Alcohol detection alarm
                position.addAlarm(Position.ALARM_GENERAL);
                position.set("alcoholDetected", true);
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("val")) {
                        try {
                            double alcoholLevel = detail.getJsonNumber("val").doubleValue();
                            position.set("alcoholLevel", alcoholLevel);
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "22":
                // Driver unique ID / Swipe card
                try {
                    JsonObject detail = json.getJsonObject("det");
                    String value = detail.getString("cn", null);
                    if (value != null && !value.isEmpty()) {
                        // Clean the driver ID: remove whitespace, line breaks, and $0$I markers
                        value = value.replaceAll("\\s+", "")              // Remove all whitespace (spaces, tabs)
                                     .replaceAll("\\r", "")               // Remove carriage returns
                                     .replaceAll("\\n", "")               // Remove newlines
                                     .replaceAll("rn", "|")               // Replace 'rn' with pipe
                                     .replaceAll("\\$0\\$I[a-z]?", "")    // Remove $0$I or $0$Ir markers
                                     .trim();
                        if (!value.isEmpty()) {
                            position.set(Position.KEY_DRIVER_UNIQUE_ID, value);
                        }
                    }
                    // Extract tp (card type) if available
                    if (detail.containsKey("tp")) {
                        position.set("cardType", detail.getInt("tp"));
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse driver unique ID", e);
                }
                break;

            // Phase 2: Trip Management
            case "19":
                // ACC on
                position.set(Position.KEY_IGNITION, true);
                position.set(Position.KEY_STATUS, "ACC_ON");
                break;

            case "31":
                // ACC off
                position.set(Position.KEY_IGNITION, false);
                position.set(Position.KEY_STATUS, "ACC_OFF");
                break;

            case "40":
                // Trip start
                position.set(Position.KEY_STATUS, "TRIP_START");
                position.set("tripState", "start");
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("tid")) {
                        try {
                            position.set("tripId", detail.getString("tid"));
                        } catch (Exception ignored) {
                        }
                    }
                    if (detail.containsKey("odo")) {
                        try {
                            double odometer = detail.getJsonNumber("odo").doubleValue();
                            position.set(Position.KEY_ODOMETER, odometer * 1000); // Convert km to meters
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "41":
                // Trip in progress
                position.set(Position.KEY_STATUS, "TRIP_IN_PROGRESS");
                position.set("tripState", "in_progress");
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("tid")) {
                        try {
                            position.set("tripId", detail.getString("tid"));
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "42":
                // Trip end
                position.set(Position.KEY_STATUS, "TRIP_END");
                position.set("tripState", "end");
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("tid")) {
                        try {
                            position.set("tripId", detail.getString("tid"));
                        } catch (Exception ignored) {
                        }
                    }
                    if (detail.containsKey("dur")) {
                        try {
                            int duration = detail.getInt("dur"); // Duration in seconds
                            position.set("tripDuration", duration);
                        } catch (Exception ignored) {
                        }
                    }
                    if (detail.containsKey("dst")) {
                        try {
                            double distance = detail.getJsonNumber("dst").doubleValue();
                            position.set("tripDistance", distance * 1000); // Convert km to meters
                        } catch (Exception ignored) {
                        }
                    }
                    if (detail.containsKey("odo")) {
                        try {
                            double odometer = detail.getJsonNumber("odo").doubleValue();
                            position.set(Position.KEY_ODOMETER, odometer * 1000); // Convert km to meters
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "43":
                // Vehicle movement alarm
                position.addAlarm(Position.ALARM_MOVEMENT);
                position.set(Position.KEY_STATUS, "VEHICLE_MOVING");
                break;

            case "46":
                // Vehicle stationary alarm
                position.set(Position.KEY_STATUS, "VEHICLE_STATIONARY");
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("dur")) {
                        try {
                            int duration = detail.getInt("dur"); // Stationary duration in seconds
                            position.set("stationaryDuration", duration);
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            // Phase 3: Geofence
            case "13":
                // Geofence alarm with multiple sub-types
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    int subType = detail.getInt("st", -1);

                    // Extract geofence information
                    if (detail.containsKey("gid")) {
                        try {
                            position.set("geofenceId", detail.getString("gid"));
                        } catch (Exception ignored) {
                        }
                    }
                    if (detail.containsKey("gnm")) {
                        try {
                            position.set("geofenceName", detail.getString("gnm"));
                        } catch (Exception ignored) {
                        }
                    }

                    // Decode based on sub-type
                    switch (subType) {
                        case 0:
                            // Enter geofence
                            position.addAlarm(Position.ALARM_GEOFENCE_ENTER);
                            position.set("geofenceEvent", "enter");
                            break;
                        case 1:
                            // Exit geofence
                            position.addAlarm(Position.ALARM_GEOFENCE_EXIT);
                            position.set("geofenceEvent", "exit");
                            break;
                        case 2:
                            // Enter line geofence
                            position.addAlarm(Position.ALARM_GEOFENCE_ENTER);
                            position.set("geofenceEvent", "enter_line");
                            position.set("geofenceType", "line");
                            break;
                        case 3:
                            // Exit line geofence
                            position.addAlarm(Position.ALARM_GEOFENCE_EXIT);
                            position.set("geofenceEvent", "exit_line");
                            position.set("geofenceType", "line");
                            break;
                        case 4:
                            // Enter polygon geofence
                            position.addAlarm(Position.ALARM_GEOFENCE_ENTER);
                            position.set("geofenceEvent", "enter_polygon");
                            position.set("geofenceType", "polygon");
                            break;
                        case 5:
                            // Exit polygon geofence
                            position.addAlarm(Position.ALARM_GEOFENCE_EXIT);
                            position.set("geofenceEvent", "exit_polygon");
                            position.set("geofenceType", "polygon");
                            break;
                        case 6:
                            // Enter circular geofence
                            position.addAlarm(Position.ALARM_GEOFENCE_ENTER);
                            position.set("geofenceEvent", "enter_circle");
                            position.set("geofenceType", "circle");
                            break;
                        case 7:
                            // Exit circular geofence
                            position.addAlarm(Position.ALARM_GEOFENCE_EXIT);
                            position.set("geofenceEvent", "exit_circle");
                            position.set("geofenceType", "circle");
                            break;
                        case 8:
                            // Overspeed in geofence
                            position.addAlarm(Position.ALARM_OVERSPEED);
                            position.set("geofenceEvent", "overspeed");
                            if (detail.containsKey("spd")) {
                                try {
                                    double speed = detail.getJsonNumber("spd").doubleValue();
                                    position.set("alarmSpeed", speed);
                                } catch (Exception ignored) {
                                }
                            }
                            if (detail.containsKey("lmt")) {
                                try {
                                    double limit = detail.getJsonNumber("lmt").doubleValue();
                                    position.set("speedLimit", limit);
                                } catch (Exception ignored) {
                                }
                            }
                            break;
                        case 9:
                            // Dwell in geofence (stayed too long)
                            position.set("geofenceEvent", "dwell");
                            if (detail.containsKey("dur")) {
                                try {
                                    int duration = detail.getInt("dur");
                                    position.set("dwellDuration", duration);
                                } catch (Exception ignored) {
                                }
                            }
                            break;
                        case 10:
                            // Route deviation
                            position.addAlarm(Position.ALARM_GENERAL);
                            position.set("geofenceEvent", "route_deviation");
                            break;
                        default:
                            // Generic geofence alarm
                            position.addAlarm(Position.ALARM_GEOFENCE);
                            break;
                    }
                } else {
                    // No detail, generic geofence alarm
                    position.addAlarm(Position.ALARM_GEOFENCE);
                }
                break;

            case "14":
                // Geofence related event
                position.addAlarm(Position.ALARM_GEOFENCE);
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("gid")) {
                        try {
                            position.set("geofenceId", detail.getString("gid"));
                        } catch (Exception ignored) {
                        }
                    }
                    if (detail.containsKey("gnm")) {
                        try {
                            position.set("geofenceName", detail.getString("gnm"));
                        } catch (Exception ignored) {
                        }
                    }
                    if (detail.containsKey("tp")) {
                        try {
                            position.set("geofenceType", detail.getInt("tp"));
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            // Phase 4: Video & Sensors
            case "1":
                // Video loss alarm
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("ch")) {
                        try {
                            int channel = detail.getInt("ch");
                            position.set("videoLossChannel", channel);
                            position.set(Position.KEY_ALARM, "videoLoss");
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    position.set(Position.KEY_ALARM, "videoLoss");
                }
                break;

            case "2":
                // Video signal occlusion
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("ch")) {
                        try {
                            int channel = detail.getInt("ch");
                            position.set("videoOcclusionChannel", channel);
                        } catch (Exception ignored) {
                        }
                    }
                }
                position.set(Position.KEY_ALARM, "videoOcclusion");
                break;

            case "3":
                // Storage/Disk failure alarm
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("tp")) {
                        try {
                            int type = detail.getInt("tp");
                            position.set("diskFailureType", type);
                        } catch (Exception ignored) {
                        }
                    }
                }
                position.addAlarm(Position.ALARM_FAULT);
                position.set("diskFailure", true);
                break;

            case "4":
                // Motion detection alarm
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("ch")) {
                        try {
                            int channel = detail.getInt("ch");
                            position.set("motionChannel", channel);
                        } catch (Exception ignored) {
                        }
                    }
                }
                position.set(Position.KEY_ALARM, "motionDetection");
                break;

            case "12":
                // G-sensor alarm with sub-types
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    int subType = detail.getInt("tp", -1);

                    switch (subType) {
                        case 1:
                            // Collision alarm
                            position.addAlarm(Position.ALARM_ACCIDENT);
                            position.set("gSensorEvent", "collision");
                            break;
                        case 2:
                            // Roll over alarm
                            position.addAlarm(Position.ALARM_ACCIDENT);
                            position.set("gSensorEvent", "rollover");
                            break;
                        case 3:
                            // Harsh acceleration
                            position.addAlarm(Position.ALARM_ACCELERATION);
                            position.set("gSensorEvent", "harsh_acceleration");
                            break;
                        case 4:
                            // Harsh braking
                            position.addAlarm(Position.ALARM_BRAKING);
                            position.set("gSensorEvent", "harsh_braking");
                            break;
                        case 5:
                            // Harsh cornering (left)
                            position.addAlarm(Position.ALARM_CORNERING);
                            position.set("gSensorEvent", "harsh_cornering_left");
                            break;
                        case 6:
                            // Harsh cornering (right)
                            position.addAlarm(Position.ALARM_CORNERING);
                            position.set("gSensorEvent", "harsh_cornering_right");
                            break;
                        default:
                            // Generic G-sensor alarm
                            position.addAlarm(Position.ALARM_GENERAL);
                            position.set("gSensorEvent", "general");
                            break;
                    }

                    // Extract G-sensor values if available
                    if (detail.containsKey("x")) {
                        try {
                            position.set("gSensorX", detail.getJsonNumber("x").doubleValue());
                        } catch (Exception ignored) {
                        }
                    }
                    if (detail.containsKey("y")) {
                        try {
                            position.set("gSensorY", detail.getJsonNumber("y").doubleValue());
                        } catch (Exception ignored) {
                        }
                    }
                    if (detail.containsKey("z")) {
                        try {
                            position.set("gSensorZ", detail.getJsonNumber("z").doubleValue());
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    position.addAlarm(Position.ALARM_GENERAL);
                }
                break;

            case "45":
                // Temperature alarm
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    int subType = detail.getInt("tp", -1);

                    switch (subType) {
                        case 1:
                            // High temperature alarm
                            position.set(Position.KEY_ALARM, "temperatureHigh");
                            position.set("temperatureEvent", "high");
                            break;
                        case 2:
                            // Low temperature alarm
                            position.set(Position.KEY_ALARM, "temperatureLow");
                            position.set("temperatureEvent", "low");
                            break;
                        default:
                            position.set(Position.KEY_ALARM, "temperature");
                            break;
                    }

                    // Extract temperature values
                    if (detail.containsKey("val")) {
                        try {
                            double temp = detail.getJsonNumber("val").doubleValue();
                            position.set(Position.PREFIX_TEMP + 1, temp);
                        } catch (Exception ignored) {
                        }
                    }
                    if (detail.containsKey("ch")) {
                        try {
                            int channel = detail.getInt("ch");
                            position.set("temperatureChannel", channel);
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    position.set(Position.KEY_ALARM, "temperature");
                }
                break;

            // Phase 6: System Status
            case "0":
                // Normal status / No alarm
                position.set(Position.KEY_STATUS, "NORMAL");
                break;

            case "6":
                // Illegal ignition
                position.addAlarm(Position.ALARM_TAMPERING);
                position.set("tamperingEvent", "illegal_ignition");
                break;

            case "8":
                // Illegal displacement (vehicle moved without authorization)
                position.addAlarm(Position.ALARM_TAMPERING);
                position.set("tamperingEvent", "illegal_displacement");
                break;

            case "9":
                // Entry alarm
                position.addAlarm(Position.ALARM_DOOR);
                position.set("doorEvent", "entry_alarm");
                break;

            case "10":
                // Low battery alarm
                position.addAlarm(Position.ALARM_LOW_BATTERY);
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("val")) {
                        try {
                            position.set(Position.KEY_BATTERY_LEVEL, detail.getJsonNumber("val").doubleValue());
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "11":
                // Power cut alarm (main power disconnected)
                position.addAlarm(Position.ALARM_POWER_CUT);
                break;

            case "15":
                // Vibration alarm
                position.addAlarm(Position.ALARM_VIBRATION);
                break;

            case "16":
                // Falling alarm
                position.addAlarm(Position.ALARM_FALL_DOWN);
                break;

            case "18":
                // Low speed alarm
                position.addAlarm(Position.ALARM_LOW_SPEED);
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("spd")) {
                        try {
                            position.set("alarmSpeed", detail.getJsonNumber("spd").doubleValue());
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "20":
                // GPS antenna cut/disconnected
                position.addAlarm(Position.ALARM_GPS_ANTENNA_CUT);
                break;

            case "21":
                // GPS antenna short circuit
                position.addAlarm(Position.ALARM_FAULT);
                position.set("faultType", "gps_antenna_short");
                break;

            case "23":
                // Device power on
                position.addAlarm(Position.ALARM_POWER_ON);
                break;

            case "26":
                // Illegal door open
                position.addAlarm(Position.ALARM_DOOR);
                position.set("doorEvent", "illegal_open");
                break;

            case "27":
                // Door open alarm
                position.addAlarm(Position.ALARM_DOOR);
                break;

            case "28":
                // Door close alarm
                position.set(Position.KEY_ALARM, "doorClose");
                break;

            case "29":
                // Input alarm (IO trigger)
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("ch")) {
                        try {
                            position.set("inputChannel", detail.getInt("ch"));
                        } catch (Exception ignored) {
                        }
                    }
                }
                position.set(Position.KEY_ALARM, "input");
                break;

            case "32":
                // Cornering alarm
                position.addAlarm(Position.ALARM_CORNERING);
                break;

            case "33":
                // Fuel leak alarm
                position.set(Position.KEY_ALARM, "fuelLeak");
                break;

            case "34":
                // Fuel theft alarm
                position.set(Position.KEY_ALARM, "fuelTheft");
                break;

            case "35":
                // Engine start
                position.set(Position.KEY_IGNITION, true);
                position.set(Position.KEY_STATUS, "ENGINE_START");
                break;

            case "36":
                // Engine stop
                position.set(Position.KEY_IGNITION, false);
                position.set(Position.KEY_STATUS, "ENGINE_STOP");
                break;

            case "38":
                // External power supply alarm
                position.set(Position.KEY_ALARM, "externalPower");
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("st")) {
                        try {
                            int status = detail.getInt("st");
                            position.set("powerStatus", status == 1 ? "connected" : "disconnected");
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "39":
                // RPM exceeds (engine speed over limit)
                position.set(Position.KEY_ALARM, Position.ALARM_HIGH_RPM);
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("rpm")) {
                        try {
                            int rpm = detail.getInt("rpm");
                            position.set(Position.KEY_RPM, rpm);
                        } catch (Exception ignored) {
                        }
                    }
                    if (detail.containsKey("lmt")) {
                        try {
                            int rpmLimit = detail.getInt("lmt");
                            position.set("rpmLimit", rpmLimit);
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "44":
                // Mileage alarm
                position.set(Position.KEY_ALARM, "mileage");
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("odo")) {
                        try {
                            double odometer = detail.getJsonNumber("odo").doubleValue();
                            position.set(Position.KEY_ODOMETER, odometer * 1000);
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "47":
                // Jamming alarm (signal jamming detected)
                position.set(Position.KEY_ALARM, "jamming");
                break;

            case "49":
                // Light sensor alarm
                position.set(Position.KEY_ALARM, "lightSensor");
                break;

            case "50":
                // Parking overtime alarm
                position.set(Position.KEY_ALARM, "parkingOvertime");
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("dur")) {
                        try {
                            position.set("parkingDuration", detail.getInt("dur"));
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "51":
                // Bluetooth disconnect alarm
                position.set(Position.KEY_ALARM, "bluetoothDisconnect");
                break;

            case "52":
                // Canbus communication failure
                position.addAlarm(Position.ALARM_FAULT);
                position.set("faultType", "canbus_failure");
                break;

            case "53":
                // SIM card not inserted
                position.set(Position.KEY_ALARM, "simNotInserted");
                break;

            case "54":
                // Network connection failure
                position.set(Position.KEY_ALARM, "networkFailure");
                break;

            case "55":
                // Positioning failure / GPS failure
                position.set(Position.KEY_ALARM, "gpsFailure");
                break;

            case "56":
                // Device malfunction
                position.addAlarm(Position.ALARM_FAULT);
                position.set("faultType", "device_malfunction");
                break;

            case "57":
                // Camera failure
                position.addAlarm(Position.ALARM_FAULT);
                position.set("faultType", "camera_failure");
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("ch")) {
                        try {
                            position.set("cameraChannel", detail.getInt("ch"));
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "58":
                // Abnormal driving time
                position.set(Position.KEY_ALARM, "abnormalDrivingTime");
                break;

            case "59":
                // Harsh turn alarm
                position.addAlarm(Position.ALARM_CORNERING);
                position.set("corneringEvent", "harsh_turn");
                break;

            case "60":
                // U-turn alarm
                position.set(Position.KEY_ALARM, "uTurn");
                break;

            case "768":
                // Trip notification
                position.set(Position.KEY_STATUS, "TRIP_NOTIFICATION");
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("tid")) {
                        try {
                            position.set("tripId", detail.getString("tid"));
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "769":
                // Tire pressure alarm
                position.set(Position.KEY_ALARM, "tirePressure");
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("tp")) {
                        try {
                            int tire = detail.getInt("tp");
                            position.set("tirePosition", tire);
                        } catch (Exception ignored) {
                        }
                    }
                    if (detail.containsKey("val")) {
                        try {
                            position.set("tirePressureValue", detail.getJsonNumber("val").doubleValue());
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            case "770":
                // Disk detection alarm
                position.set(Position.KEY_ALARM, "diskDetection");
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    if (detail.containsKey("st")) {
                        try {
                            position.set("diskStatus", detail.getInt("st"));
                        } catch (Exception ignored) {
                        }
                    }
                }
                break;

            // Phase 5: ADAS/DMS/BSD
            case "30":
                // Advanced Driver Assistance System alarms
                if (json.containsKey("det")) {
                    JsonObject detail = json.getJsonObject("det");
                    int subType = detail.getInt("tp", -1);

                    // Extract common ADAS data
                    if (detail.containsKey("spd")) {
                        try {
                            position.set("adasSpeed", detail.getJsonNumber("spd").doubleValue());
                        } catch (Exception ignored) {
                        }
                    }

                    switch (subType) {
                        // ADAS Alarms (tp=1-15)
                        case 1:
                            position.set(Position.KEY_ALARM, "forwardCollision");
                            position.set("adasEvent", "forward_collision_warning");
                            break;
                        case 2:
                            position.set(Position.KEY_ALARM, "laneDeparture");
                            position.set("adasEvent", "lane_departure_warning");
                            break;
                        case 3:
                            position.set(Position.KEY_ALARM, "pedestrianCollision");
                            position.set("adasEvent", "pedestrian_collision_warning");
                            break;
                        case 4:
                            position.set(Position.KEY_ALARM, "headwayWarning");
                            position.set("adasEvent", "distance_monitoring_warning");
                            break;
                        case 5:
                            position.set(Position.KEY_ALARM, "roadSignRecognition");
                            position.set("adasEvent", "road_sign_recognition");
                            break;
                        case 6:
                            position.set(Position.KEY_ALARM, "laneChange");
                            position.set("adasEvent", "frequent_lane_change");
                            break;
                        case 7:
                            position.set(Position.KEY_ALARM, "vehicleDistance");
                            position.set("adasEvent", "vehicle_distance_warning");
                            break;
                        case 8:
                            position.set(Position.KEY_ALARM, "pedestrianDetection");
                            position.set("adasEvent", "pedestrian_detection");
                            break;
                        case 9:
                            position.set(Position.KEY_ALARM, "adasFailure");
                            position.set("adasEvent", "adas_failure");
                            break;
                        case 10:
                            position.set(Position.KEY_ALARM, "obstacleWarning");
                            position.set("adasEvent", "obstacle_warning");
                            break;
                        case 11:
                            position.set(Position.KEY_ALARM, "blindSpotWarning");
                            position.set("adasEvent", "blind_spot_warning");
                            break;
                        case 12:
                            position.set(Position.KEY_ALARM, "rearCollision");
                            position.set("adasEvent", "rear_collision_warning");
                            break;
                        case 13:
                            position.set(Position.KEY_ALARM, "emergencyBraking");
                            position.set("adasEvent", "automatic_emergency_braking");
                            break;
                        case 14:
                            position.set(Position.KEY_ALARM, "trafficSignViolation");
                            position.set("adasEvent", "traffic_sign_violation");
                            break;
                        case 15:
                            position.set(Position.KEY_ALARM, "laneKeepingAssist");
                            position.set("adasEvent", "lane_keeping_assist");
                            break;

                        // DMS Alarms (tp=16-30)
                        case 16:
                            position.set(Position.KEY_ALARM, "driverFatigue");
                            position.set("dmsEvent", "driver_fatigue");
                            break;
                        case 17:
                            position.set(Position.KEY_ALARM, "driverDistraction");
                            position.set("dmsEvent", "driver_distraction");
                            break;
                        case 18:
                            position.set(Position.KEY_ALARM, "phoneCall");
                            position.set("dmsEvent", "phone_call_detected");
                            break;
                        case 19:
                            position.set(Position.KEY_ALARM, "smoking");
                            position.set("dmsEvent", "smoking_detected");
                            break;
                        case 20:
                            position.set(Position.KEY_ALARM, "driverAbsent");
                            position.set("dmsEvent", "driver_absent");
                            break;
                        case 21:
                            position.set(Position.KEY_ALARM, "yawning");
                            position.set("dmsEvent", "yawning_detected");
                            break;
                        case 22:
                            position.set(Position.KEY_ALARM, "eyesClosed");
                            position.set("dmsEvent", "eyes_closed");
                            break;
                        case 23:
                            position.set(Position.KEY_ALARM, "headDown");
                            position.set("dmsEvent", "head_down");
                            break;
                        case 24:
                            position.set(Position.KEY_ALARM, "abnormalDriving");
                            position.set("dmsEvent", "abnormal_driving_behavior");
                            break;
                        case 25:
                            position.set(Position.KEY_ALARM, "dmsFailure");
                            position.set("dmsEvent", "dms_failure");
                            break;
                        case 26:
                            position.set(Position.KEY_ALARM, "faceNotDetected");
                            position.set("dmsEvent", "face_not_detected");
                            break;
                        case 27:
                            position.set(Position.KEY_ALARM, "cameraBlocked");
                            position.set("dmsEvent", "camera_blocked");
                            break;
                        case 28:
                            position.set(Position.KEY_ALARM, "infraredFailure");
                            position.set("dmsEvent", "infrared_light_failure");
                            break;
                        case 29:
                            position.set(Position.KEY_ALARM, "seatbeltNotFastened");
                            position.set("dmsEvent", "seatbelt_not_fastened");
                            break;
                        case 30:
                            position.set(Position.KEY_ALARM, "driverChange");
                            position.set("dmsEvent", "driver_change_detected");
                            break;

                        // BSD Alarms (tp=31-39)
                        case 31:
                            position.set(Position.KEY_ALARM, "bsdLeftWarning");
                            position.set("bsdEvent", "left_blind_spot_warning");
                            break;
                        case 32:
                            position.set(Position.KEY_ALARM, "bsdRightWarning");
                            position.set("bsdEvent", "right_blind_spot_warning");
                            break;
                        case 33:
                            position.set(Position.KEY_ALARM, "laneChangeLeft");
                            position.set("bsdEvent", "lane_change_left_warning");
                            break;
                        case 34:
                            position.set(Position.KEY_ALARM, "laneChangeRight");
                            position.set("bsdEvent", "lane_change_right_warning");
                            break;
                        case 35:
                            position.set(Position.KEY_ALARM, "rearCrossingLeft");
                            position.set("bsdEvent", "rear_crossing_left_warning");
                            break;
                        case 36:
                            position.set(Position.KEY_ALARM, "rearCrossingRight");
                            position.set("bsdEvent", "rear_crossing_right_warning");
                            break;
                        case 37:
                            position.set(Position.KEY_ALARM, "bsdFailure");
                            position.set("bsdEvent", "bsd_system_failure");
                            break;
                        case 38:
                            position.set(Position.KEY_ALARM, "doorOpenWarning");
                            position.set("bsdEvent", "door_open_warning");
                            break;
                        case 39:
                            position.set(Position.KEY_ALARM, "parkingAssist");
                            position.set("bsdEvent", "parking_assist_warning");
                            break;

                        default:
                            position.set(Position.KEY_ALARM, "adas");
                            position.set("adasEvent", "unknown");
                            break;
                    }

                    // Extract additional ADAS/DMS data
                    if (detail.containsKey("lvl")) {
                        try {
                            position.set("alarmLevel", detail.getInt("lvl"));
                        } catch (Exception ignored) {
                        }
                    }
                    if (detail.containsKey("dst")) {
                        try {
                            position.set("objectDistance", detail.getJsonNumber("dst").doubleValue());
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    position.set(Position.KEY_ALARM, "adas");
                }
                break;

            default:
                // Unknown event code
                position.addAlarm(Position.ALARM_GENERAL);
                break;
        }
    }

    private void decodeStatusData(Position position, ByteBuf buf) {

        if (buf == null || buf.readableBytes() < 6) {
            return;
        }

    Date deviceTime = applyTimeOffset(readTimestamp(buf));
        position.setDeviceTime(deviceTime);
        position.setTime(deviceTime);

        int content = buf.readUnsignedShortLE();

        if (BitUtil.check(content, 0)) {
            decodeLocation(position, buf);
        } else {
            getLastLocation(position, deviceTime);
        }

        if (BitUtil.check(content, 1)) {
            decodeGSensor(position, buf, content);
        }

        if (BitUtil.check(content, 2) && buf.readableBytes() >= 4) {
            decodeBasicStatus(position, buf);
        }

        if (BitUtil.check(content, 3) && buf.readableBytes() >= 8) {
            decodeModuleStatus(position, buf);
        }

        if (BitUtil.check(content, 8) && buf.readableBytes() >= 5) {
            decodeMobileStatus(position, buf);
        }

        if (BitUtil.check(content, 5) && buf.readableBytes() >= 11) {
            decodeStorageStatus(position, buf);
        }

        if (BitUtil.check(content, 9) && buf.readableBytes() >= 12) {
            decodeAlarmStatus(position, buf);
        }

        if (BitUtil.check(content, 7) && buf.readableBytes() >= 12) {
            decodeTemperatureStatus(position, buf);
        }

        if (buf.isReadable()) {
            buf.skipBytes(buf.readableBytes());
        }
    }

    private void decodeGSensor(Position position, ByteBuf buf, int contentMask) {
        if (!buf.isReadable()) {
            return;
        }

        int identifier = buf.readUnsignedByte();
        position.set("GSensorMask", identifier);

        int required = 0;
        if (BitUtil.check(contentMask, 2)) {
            required += 4;
        }
        if (BitUtil.check(contentMask, 3)) {
            required += 8;
        }
        if (BitUtil.check(contentMask, 8)) {
            required += 5;
        }
        if (BitUtil.check(contentMask, 5)) {
            required += 11;
        }
        if (BitUtil.check(contentMask, 9)) {
            required += 12;
        }
        if (BitUtil.check(contentMask, 7)) {
            required += 12;
        }

        int available = buf.readableBytes() - required;
        if (available < 0) {
            available = 0;
        }

        int valuesToRead = Math.min(5, available / 2);
        String[] keys = {"AccelX", "AccelY", "AccelZ", "Tilt", "Impact"};

        for (int i = 0; i < valuesToRead; i++) {
            if (buf.readableBytes() < 2) {
                break;
            }
            short value = buf.readShortLE();
            position.set(keys[i], value);
        }
    }

    private void decodeModuleStatus(Position position, ByteBuf buf) {
        if (buf.readableBytes() < 8) {
            return;
        }

        int mask = buf.readUnsignedShortLE();
        int mobile = buf.readUnsignedByte();
        int location = buf.readUnsignedByte();
        int wifi = buf.readUnsignedByte();
        int gSensor = buf.readUnsignedByte();
        int recording = buf.readUnsignedShortLE();

        position.set("Mask", mask);
        position.set("Mobile", mobile);
        position.set("Location", location);
        position.set("Wifi", wifi);
        position.set("GSensor", gSensor);
        position.set("Recording", recording);
    }

    private void decodeMobileStatus(Position position, ByteBuf buf) {
        if (buf.readableBytes() < 5) {
            return;
        }

        int mask = buf.readUnsignedByte();
        int signal = buf.readUnsignedByte();
        int networkType = buf.readUnsignedByte();
        buf.skipBytes(Math.min(2, buf.readableBytes()));

        position.set("MobileMask", mask);
        position.set("Signal", signal);
        position.set(Position.KEY_RSSI, signal);
        position.set("NetworkType", networkType);
    }

    private void decodeStorageStatus(Position position, ByteBuf buf) {
        if (buf.readableBytes() < 11) {
            return;
        }

        int mask = buf.readUnsignedByte();
        int diskNumber = buf.readUnsignedByte();
        int diskStatus = buf.readUnsignedByte();
        long diskSize = buf.readUnsignedIntLE();
        long diskAvailable = buf.readUnsignedIntLE();

        position.set("StorageMask", mask);
        position.set("StorageDisk", diskNumber);
        position.set("StorageStatus", diskStatus);
        position.set("StorageSize", diskSize);
        position.set("StorageAvailable", diskAvailable);
    }

    private void decodeAlarmStatus(Position position, ByteBuf buf) {
        if (buf.readableBytes() < 12) {
            return;
        }

        long mask = buf.readUnsignedIntLE();
        int videoLoss = buf.readUnsignedShortLE();
        int motionDetection = buf.readUnsignedShortLE();
        int videoCover = buf.readUnsignedShortLE();
        int inputTrigger = buf.readUnsignedShortLE();

        position.set("AlarmMask", mask);
        position.set("AlarmVideoLoss", videoLoss);
        position.set("AlarmMotion", motionDetection);
        position.set("AlarmCover", videoCover);
        position.set("AlarmInput", inputTrigger);
    }

    private void decodeTemperatureStatus(Position position, ByteBuf buf) {
        if (buf.readableBytes() < 12) {
            return;
        }

        int mask = buf.readUnsignedShortLE();
        int tempIn = buf.readShortLE();
        int tempOut = buf.readShortLE();
        int tempEngine = buf.readShortLE();
        int tempDevice = buf.readShortLE();
        int humidityIn = buf.readUnsignedByte();
        int humidityOut = buf.readUnsignedByte();

        position.set("TempMask", mask);
        position.set("TempIn", tempIn);
        position.set("TempOut", tempOut);
        position.set("TempEngine", tempEngine);
        position.set("TempDevice", tempDevice);
        position.set("HumidityIn", humidityIn);
        position.set("HumidityOut", humidityOut);
    }

    private void applyJsonDerivedValues(Position position, JsonObject json) {

        JsonObject detail = null;
        if (json.containsKey("det")) {
            JsonValue value = json.get("det");
            if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                detail = json.getJsonObject("det");
            }
        }

        if (!position.getValid()) {
            Double latitude = parseJsonDouble(json, "lat", "latitude", "lt");
            Double longitude = parseJsonDouble(json, "lng", "longitude", "ln", "lon");

            if (latitude == null && detail != null) {
                latitude = parseJsonDouble(detail, "lat", "latitude", "slat", "clat");
            }

            if (longitude == null && detail != null) {
                longitude = parseJsonDouble(detail, "lng", "longitude", "slng", "clng");
            }

            if (latitude != null && longitude != null && (latitude != 0.0 || longitude != 0.0)) {
                position.setLatitude(latitude);
                position.setLongitude(longitude);
                position.setValid(true);
            }
        }

        if (position.getSpeed() == 0) {
            Double speed = parseJsonDouble(json, "spd", "speed", "spds");
            if (speed == null && detail != null) {
                speed = parseJsonDouble(detail, "spd", "speed", "spds");
            }
            if (speed != null) {
                position.setSpeed(UnitsConverter.knotsFromKph(speed));
            }
        }
    }

    private Double parseJsonDouble(JsonObject source, String... keys) {
        for (String key : keys) {
            if (!source.containsKey(key)) {
                continue;
            }
            JsonValue value = source.get(key);
            if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
                continue;
            }
            if (value.getValueType() == JsonValue.ValueType.NUMBER) {
                return ((JsonNumber) value).doubleValue();
            }
            if (value.getValueType() == JsonValue.ValueType.STRING) {
                String text = source.getString(key, null);
                Double parsed = parseDouble(text);
                if (parsed != null) {
                    return parsed;
                }
            }
        }
        return null;
    }

    private Double parseDouble(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void decodeLocation(Position position, ByteBuf buf) {

        int info = buf.readUnsignedByte();
        int locationType = buf.readUnsignedByte();

    Date fixTime = applyTimeOffset(readTimestamp(buf));
        position.setTime(fixTime);
        position.setValid(locationType != 0);
        position.set(Position.KEY_STATUS, locationType);

        int direction = buf.readUnsignedByte();
        int satellites = buf.readUnsignedByte();
        int speed = buf.readUnsignedShortLE();
        int altitudeRaw = buf.readShortLE();
        int accuracy = buf.readUnsignedShortLE();

        int longitudeDegrees = buf.readUnsignedByte();
        long longitudeMinutes = buf.readUnsignedIntLE();
        int latitudeDegrees = buf.readUnsignedByte();
        long latitudeMinutes = buf.readUnsignedIntLE();

        double longitude = longitudeDegrees + longitudeMinutes / 600000.0;
        if (BitUtil.check(info, 1)) {
            longitude = -longitude;
        }

        double latitude = latitudeDegrees + latitudeMinutes / 600000.0;
        if (BitUtil.check(info, 4)) {
            latitude = -latitude;
        }

        position.setLongitude(longitude);
        position.setLatitude(latitude);

        double course = direction;
        if (BitUtil.check(info, 0)) {
            course += 180.0;
        }
        position.setCourse(course % 360.0);

        position.set(Position.KEY_SATELLITES, satellites);
        position.setSpeed(UnitsConverter.knotsFromKph(speed / 100.0));

        double altitude = altitudeRaw / 100.0;
        if (BitUtil.check(info, 2)) {
            altitude = -altitude;
        }
        position.setAltitude(altitude);

        position.setAccuracy(accuracy / 10.0);
    }

    private void decodeBasicStatus(Position position, ByteBuf buf) {
        int status1 = buf.readUnsignedByte();
        int status2 = buf.readUnsignedByte();
        buf.skipBytes(2);

        position.set(Position.KEY_IGNITION, BitUtil.check(status1, 0));
        boolean doorOpen = BitUtil.check(status1, 6) || BitUtil.check(status1, 7)
                || BitUtil.check(status2, 0) || BitUtil.check(status2, 1)
                || BitUtil.check(status2, 2) || BitUtil.check(status2, 3);
        position.set(Position.KEY_DOOR, doorOpen);
    }

    private Date readTimestamp(ByteBuf buf) {
        int year = buf.readUnsignedByte();
        int month = buf.readUnsignedByte();
        int day = buf.readUnsignedByte();
        int hour = buf.readUnsignedByte();
        int minute = buf.readUnsignedByte();
        int second = buf.readUnsignedByte();
        return new DateBuilder()
                .setDate(year, month, day)
                .setTime(hour, minute, second)
                .getDate();
    }

    private Date applyTimeOffset(Date date) {
        if (date == null) {
            return null;
        }
        return new Date(date.getTime() - TIME_OFFSET_MILLIS);
    }

    private String readString(ByteBuf buf) {
        return readString(buf, buf.readableBytes());
    }

    private String readString(ByteBuf buf, int length) {
        byte[] data = new byte[length];
        buf.readBytes(data);
        int actualLength = data.length;
        while (actualLength > 0) {
            byte last = data[actualLength - 1];
            if (last == 0 || last == '\n' || last == '\r') {
                actualLength -= 1;
            } else {
                break;
            }
        }
        return new String(data, 0, actualLength, StandardCharsets.US_ASCII);
    }

    private void sendJsonResponse(Channel channel, SocketAddress remoteAddress, int type, JsonObject json) {
        if (channel != null) {
            sendResponse(channel, remoteAddress, type, encodeJson(json));
        }
    }

    private void sendResponse(Channel channel, SocketAddress remoteAddress, int type, byte[] payload) {
        if (channel != null) {
            int length = payload != null ? payload.length : 0;
            ByteBuf response = channel.alloc().buffer(8 + length);
            response.writeByte(HEADER_FLAG);
            response.writeByte(PROTOCOL_VERSION);
            response.writeShortLE(type);
            response.writeIntLE(length);
            if (payload != null) {
                response.writeBytes(payload);
            }
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private byte[] encodeJson(JsonObject json) {
        byte[] content = (json.toString() + "\n").getBytes(StandardCharsets.US_ASCII);
        byte[] payload = Arrays.copyOf(content, content.length + 1);
        payload[payload.length - 1] = 0;
        return payload;
    }
}
