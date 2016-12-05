/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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
package org.traccar.notification;

import java.text.DecimalFormat;
import java.util.Formatter;
import java.util.Locale;

import org.traccar.Context;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

public final class NotificationFormatter {

    private NotificationFormatter() {
    }

    public static final String TITLE_TEMPLATE_TYPE_COMMAND_RESULT = "%1$s: command result received";
    public static final String MESSAGE_TEMPLATE_TYPE_COMMAND_RESULT = "Device: %1$s%n"
            + "Result: %3$s%n"
            + "Time: %2$tc%n";

    public static final String TITLE_TEMPLATE_TYPE_DEVICE_ONLINE = "%1$s: online";
    public static final String MESSAGE_TEMPLATE_TYPE_DEVICE_ONLINE = "Device: %1$s%n"
            + "Online%n"
            + "Time: %2$tc%n";
    public static final String TITLE_TEMPLATE_TYPE_DEVICE_UNKNOWN = "%1$s: status is unknown";
    public static final String MESSAGE_TEMPLATE_TYPE_DEVICE_UNKNOWN = "Device: %1$s%n"
            + "Status is unknown%n"
            + "Time: %2$tc%n";
    public static final String TITLE_TEMPLATE_TYPE_DEVICE_OFFLINE = "%1$s: offline";
    public static final String MESSAGE_TEMPLATE_TYPE_DEVICE_OFFLINE = "Device: %1$s%n"
            + "Offline%n"
            + "Time: %2$tc%n";

    public static final String TITLE_TEMPLATE_TYPE_DEVICE_MOVING = "%1$s: moving";
    public static final String MESSAGE_TEMPLATE_TYPE_DEVICE_MOVING = "Device: %1$s%n"
            + "Moving%n"
            + "Point: https://www.openstreetmap.org/?mlat=%3$f&mlon=%4$f#map=16/%3$f/%4$f%n"
            + "Time: %2$tc%n";
    public static final String TITLE_TEMPLATE_TYPE_DEVICE_STOPPED = "%1$s: stopped";
    public static final String MESSAGE_TEMPLATE_TYPE_DEVICE_STOPPED = "Device: %1$s%n"
            + "Stopped%n"
            + "Point: https://www.openstreetmap.org/?mlat=%3$f&mlon=%4$f#map=16/%3$f/%4$f%n"
            + "Time: %2$tc%n";

    public static final String TITLE_TEMPLATE_TYPE_DEVICE_OVERSPEED = "%1$s: exceeds the speed";
    public static final String MESSAGE_TEMPLATE_TYPE_DEVICE_OVERSPEED = "Device: %1$s%n"
            + "Exceeds the speed: %5$s%n"
            + "Point: https://www.openstreetmap.org/?mlat=%3$f&mlon=%4$f#map=16/%3$f/%4$f%n"
            + "Time: %2$tc%n";

    public static final String TITLE_TEMPLATE_TYPE_GEOFENCE_ENTER = "%1$s: has entered geofence";
    public static final String MESSAGE_TEMPLATE_TYPE_GEOFENCE_ENTER = "Device: %1$s%n"
            + "Has entered geofence: %5$s%n"
            + "Point: https://www.openstreetmap.org/?mlat=%3$f&mlon=%4$f#map=16/%3$f/%4$f%n"
            + "Time: %2$tc%n";
    public static final String TITLE_TEMPLATE_TYPE_GEOFENCE_EXIT = "%1$s: has exited geofence";
    public static final String MESSAGE_TEMPLATE_TYPE_GEOFENCE_EXIT = "Device: %1$s%n"
            + "Has exited geofence: %5$s%n"
            + "Point: https://www.openstreetmap.org/?mlat=%3$f&mlon=%4$f#map=16/%3$f/%4$f%n"
            + "Time: %2$tc%n";

    public static final String TITLE_TEMPLATE_TYPE_ALARM = "%1$s: alarm!";
    public static final String MESSAGE_TEMPLATE_TYPE_ALARM = "Device: %1$s%n"
            + "Alarm: %5$s%n"
            + "Point: https://www.openstreetmap.org/?mlat=%3$f&mlon=%4$f#map=16/%3$f/%4$f%n"
            + "Time: %2$tc%n";

    public static final String TITLE_TEMPLATE_TYPE_IGNITION_ON = "%1$s: ignition ON";
    public static final String MESSAGE_TEMPLATE_TYPE_IGNITION_ON = "Device: %1$s%n"
            + "Ignition ON%n"
            + "Point: https://www.openstreetmap.org/?mlat=%3$f&mlon=%4$f#map=16/%3$f/%4$f%n"
            + "Time: %2$tc%n";
    public static final String TITLE_TEMPLATE_TYPE_IGNITION_OFF = "%1$s: ignition OFF";
    public static final String MESSAGE_TEMPLATE_TYPE_IGNITION_OFF = "Device: %1$s%n"
            + "Ignition OFF%n"
            + "Point: https://www.openstreetmap.org/?mlat=%3$f&mlon=%4$f#map=16/%3$f/%4$f%n"
            + "Time: %2$tc%n";
    public static final String TITLE_TEMPLATE_TYPE_MAINTENANCE = "%1$s: maintenance is required";
    public static final String MESSAGE_TEMPLATE_TYPE_MAINTENANCE = "Device: %1$s%n"
            + "Maintenance is required%n"
            + "Point: https://www.openstreetmap.org/?mlat=%3$f&mlon=%4$f#map=16/%3$f/%4$f%n"
            + "Time: %2$tc%n";

    public static String formatTitle(long userId, Event event, Position position) {
        Device device = Context.getIdentityManager().getDeviceById(event.getDeviceId());
        StringBuilder stringBuilder = new StringBuilder();
        Formatter formatter = new Formatter(stringBuilder, Locale.getDefault());

        switch (event.getType()) {
            case Event.TYPE_COMMAND_RESULT:
                formatter.format(TITLE_TEMPLATE_TYPE_COMMAND_RESULT, device.getName());
                break;
            case Event.TYPE_DEVICE_ONLINE:
                formatter.format(TITLE_TEMPLATE_TYPE_DEVICE_ONLINE, device.getName());
                break;
            case Event.TYPE_DEVICE_UNKNOWN:
                formatter.format(TITLE_TEMPLATE_TYPE_DEVICE_UNKNOWN, device.getName());
                break;
            case Event.TYPE_DEVICE_OFFLINE:
                formatter.format(TITLE_TEMPLATE_TYPE_DEVICE_OFFLINE, device.getName());
                break;
            case Event.TYPE_DEVICE_MOVING:
                formatter.format(TITLE_TEMPLATE_TYPE_DEVICE_MOVING, device.getName());
                break;
            case Event.TYPE_DEVICE_STOPPED:
                formatter.format(TITLE_TEMPLATE_TYPE_DEVICE_STOPPED, device.getName());
                break;
            case Event.TYPE_DEVICE_OVERSPEED:
                formatter.format(TITLE_TEMPLATE_TYPE_DEVICE_OVERSPEED, device.getName());
                break;
            case Event.TYPE_GEOFENCE_ENTER:
                formatter.format(TITLE_TEMPLATE_TYPE_GEOFENCE_ENTER, device.getName());
                break;
            case Event.TYPE_GEOFENCE_EXIT:
                formatter.format(TITLE_TEMPLATE_TYPE_GEOFENCE_EXIT, device.getName());
                break;
            case Event.TYPE_ALARM:
                formatter.format(TITLE_TEMPLATE_TYPE_ALARM, device.getName());
                break;
            case Event.TYPE_IGNITION_ON:
                formatter.format(TITLE_TEMPLATE_TYPE_IGNITION_ON, device.getName());
                break;
            case Event.TYPE_IGNITION_OFF:
                formatter.format(TITLE_TEMPLATE_TYPE_IGNITION_OFF, device.getName());
                break;
            case Event.TYPE_MAINTENANCE:
                formatter.format(TITLE_TEMPLATE_TYPE_MAINTENANCE, device.getName());
                break;
            default:
                formatter.format("Unknown type");
                break;
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    public static String formatMessage(long userId, Event event, Position position) {
        Device device = Context.getIdentityManager().getDeviceById(event.getDeviceId());
        StringBuilder stringBuilder = new StringBuilder();
        Formatter formatter = new Formatter(stringBuilder, Locale.getDefault());

        switch (event.getType()) {
            case Event.TYPE_COMMAND_RESULT:
                formatter.format(MESSAGE_TEMPLATE_TYPE_COMMAND_RESULT, device.getName(), event.getServerTime(),
                        position.getAttributes().get(Position.KEY_RESULT));
                break;
            case Event.TYPE_DEVICE_ONLINE:
                formatter.format(MESSAGE_TEMPLATE_TYPE_DEVICE_ONLINE, device.getName(), event.getServerTime());
                break;
            case Event.TYPE_DEVICE_UNKNOWN:
                formatter.format(MESSAGE_TEMPLATE_TYPE_DEVICE_UNKNOWN, device.getName(), event.getServerTime());
                break;
            case Event.TYPE_DEVICE_OFFLINE:
                formatter.format(MESSAGE_TEMPLATE_TYPE_DEVICE_OFFLINE, device.getName(), event.getServerTime());
                break;
            case Event.TYPE_DEVICE_MOVING:
                formatter.format(MESSAGE_TEMPLATE_TYPE_DEVICE_MOVING, device.getName(), position.getFixTime(),
                        position.getLatitude(), position.getLongitude());
                break;
            case Event.TYPE_DEVICE_STOPPED:
                formatter.format(MESSAGE_TEMPLATE_TYPE_DEVICE_STOPPED, device.getName(), position.getFixTime(),
                        position.getLatitude(), position.getLongitude());
                break;
            case Event.TYPE_DEVICE_OVERSPEED:
                formatter.format(MESSAGE_TEMPLATE_TYPE_DEVICE_OVERSPEED, device.getName(), position.getFixTime(),
                        position.getLatitude(), position.getLongitude(), formatSpeed(userId, position.getSpeed()));
                break;
            case Event.TYPE_GEOFENCE_ENTER:
                formatter.format(MESSAGE_TEMPLATE_TYPE_GEOFENCE_ENTER, device.getName(), position.getFixTime(),
                        position.getLatitude(), position.getLongitude(),
                        Context.getGeofenceManager().getGeofence(event.getGeofenceId()).getName());
                break;
            case Event.TYPE_GEOFENCE_EXIT:
                formatter.format(MESSAGE_TEMPLATE_TYPE_GEOFENCE_EXIT, device.getName(), position.getFixTime(),
                        position.getLatitude(), position.getLongitude(),
                        Context.getGeofenceManager().getGeofence(event.getGeofenceId()).getName());
                break;
            case Event.TYPE_ALARM:
                formatter.format(MESSAGE_TEMPLATE_TYPE_ALARM, device.getName(), event.getServerTime(),
                        position.getLatitude(), position.getLongitude(),
                        position.getAttributes().get(Position.KEY_ALARM));
                break;
            case Event.TYPE_IGNITION_ON:
                formatter.format(MESSAGE_TEMPLATE_TYPE_IGNITION_ON, device.getName(), position.getFixTime(),
                        position.getLatitude(), position.getLongitude());
                break;
            case Event.TYPE_IGNITION_OFF:
                formatter.format(MESSAGE_TEMPLATE_TYPE_IGNITION_OFF, device.getName(), position.getFixTime(),
                        position.getLatitude(), position.getLongitude());
                break;
            case Event.TYPE_MAINTENANCE:
                formatter.format(MESSAGE_TEMPLATE_TYPE_MAINTENANCE, device.getName(), position.getFixTime(),
                        position.getLatitude(), position.getLongitude());
                break;
            default:
                formatter.format("Unknown type");
                break;
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    private static String formatSpeed(long userId, double speed) {
        DecimalFormat df = new DecimalFormat("#.##");
        String result = df.format(speed) + " kn";
        switch (Context.getPermissionsManager().getUser(userId).getSpeedUnit()) {
            case "kmh":
                result = df.format(UnitsConverter.kphFromKnots(speed)) + " km/h";
                break;
            case "mph":
                result = df.format(UnitsConverter.mphFromKnots(speed)) + " mph";
                break;
            default:
                break;
        }
        return result;
    }
}
