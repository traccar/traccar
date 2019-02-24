/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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
package org.traccar.config;

public final class Keys {

    public static final ConfigSuffix PROTOCOL_TIMEOUT = new ConfigSuffix(
            ".timeout",
            Integer.class,
            "Connection timeout value in seconds. Because sometimes there is no way to detect lost TCP connection, "
                    + "old connections stay in open state. On most systems there is a limit on number of open "
                    + "connection, so this leads to problems with establishing new connections when number of "
                    + "devices is high or devices data connections are unstable.");

    public static final ConfigKey SERVER_TIMEOUT = new ConfigKey(
            "server.timeout",
            Integer.class,
            "Server wide connection timeout value in seconds. See protocol timeout for more information.");

    public static final ConfigKey SERVER_STATISTICS = new ConfigKey(
            "server.statistics",
            Boolean.class,
            "Address for uploading aggregated anonymous usage statistics. Uploaded information is the same you can see "
                    + "on the statistics screen in the web app. It does not include any sensitive (e.g. locations).");

    public static final ConfigKey EXTRA_HANDLERS = new ConfigKey(
            "extra.handlers",
            String.class,
            "List of external handler classes to use in Netty pipeline.");

    public static final ConfigKey FORWARD_ENABLE = new ConfigKey(
            "forward.enable",
            Boolean.class,
            "Enable positions forwarding to other web server.");

    public static final ConfigKey FORWARD_URL = new ConfigKey(
            "forward.url",
            String.class,
            "URL to forward positions. Data is passed through URL parameters. For example, {uniqueId} for device "
                    + "identifier, {latitude} and {longitude} for coordinates.");

    public static final ConfigKey FORWARD_HEADER = new ConfigKey(
            "forward.header",
            String.class,
            "Additional HTTP header, can be used for authorization.");

    public static final ConfigKey FORWARD_JSON = new ConfigKey(
            "forward.json",
            Boolean.class,
            "Boolean value to enable forwarding in JSON format.");

    public static final ConfigKey FILTER_ENABLE = new ConfigKey(
            "filter.enable",
            Boolean.class,
            "Boolean flag to enable or disable position filtering.");

    public static final ConfigKey FILTER_INVALID = new ConfigKey(
            "filter.invalid",
            Boolean.class,
            "Filter invalid (valid field is set to false) positions.");

    public static final ConfigKey FILTER_ZERO = new ConfigKey(
            "filter.zero",
            Boolean.class,
            "Filter zero coordinates. Zero latitude and longitude are theoretically valid values, but it practice it "
                    + "usually indicates invalid GPS data.");

    public static final ConfigKey FILTER_DUPLICATE = new ConfigKey(
            "filter.duplicate",
            Boolean.class,
            "Filter duplicate records (duplicates are detected by time value).");

    public static final ConfigKey FILTER_FUTURE = new ConfigKey(
            "filter.future",
            Long.class,
            "Filter records with fix time in future. The values is specified in seconds. Records that have fix time "
                    + "more than specified number of seconds later than current server time would be filtered out.");

    public static final ConfigKey FILTER_ACCURACY = new ConfigKey(
            "filter.accuracy",
            Integer.class,
            "Filter positions with accuracy less than specified value in meters.");

    public static final ConfigKey FILTER_APPROXIMATE = new ConfigKey(
            "filter.approximate",
            Boolean.class,
            "Filter cell and wifi locations that are coming from geolocation provider.");

    public static final ConfigKey FILTER_STATIC = new ConfigKey(
            "filter.static",
            Boolean.class,
            "Filter positions with exactly zero speed values.");

    public static final ConfigKey FILTER_DISTANCE = new ConfigKey(
            "filter.distance",
            Integer.class,
            "Filter records by distance. The values is specified in meters. If the new position is less far than this "
                    + "value from the last one it gets filtered out.");

    public static final ConfigKey FILTER_MAX_SPEED = new ConfigKey(
            "filter.maxSpeed",
            Integer.class,
            "Filter records by Maximum Speed value in knots. Can be used to filter jumps to far locations even if "
                    + "they're marked as valid. Shouldn't be too low. Start testing with values at about 25000.");

    public static final ConfigKey FILTER_MIN_PERIOD = new ConfigKey(
            "filter.minPeriod",
            Integer.class,
            "Filter position if time from previous position is less than specified value in seconds.");

    public static final ConfigKey FILTER_SKIP_LIMIT = new ConfigKey(
            "filter.skipLimit",
            Long.class,
            "Time limit for the filtering in seconds. If the time difference between last position and a new one is "
                    + "more than this limit, the new position will not be filtered out.");

    public static final ConfigKey FILTER_SKIP_ATTRIBUTES_ENABLE = new ConfigKey(
            "filter.skipAttributes.enable",
            Boolean.class,
            "Enable attributes skipping. Attribute skipping can be enabled in the config or device attributes");

    public static final ConfigKey COORDINATES_FILTER = new ConfigKey(
            "coordinates.filter",
            Boolean.class,
            "Replaces coordinates with last known if change is less than a 'coordinates.error' meters. Helps to avoid "
                    + "coordinates jumps during parking period.");

    public static final ConfigKey COORDINATES_MIN_ERROR = new ConfigKey(
            "coordinates.minError",
            Integer.class,
            "Distance in meters. Distances below this value gets handled like explained in 'coordinates.filter'.");

    public static final ConfigKey COORDINATES_MAX_ERROR = new ConfigKey(
            "filter.maxError",
            Integer.class,
            "Distance in meters. Distances above this value gets handled like explained in 'coordinates.filter', but "
                    + "only if Position is also marked as 'invalid'.");

    public static final ConfigKey PROCESSING_REMOTE_ADDRESS_ENABLE = new ConfigKey(
            "processing.remoteAddress.enable",
            Boolean.class,
            "Enable to save device IP addresses information. Disabled by default.");

    public static final ConfigKey GEOLOCATION_ENABLE = new ConfigKey(
            "geolocation.enable",
            Boolean.class,
            "Boolean flag to enable LBS location resolution. Some devices send cell towers information and WiFi point "
                    + "when GPS location is not available. Traccar can determine coordinates based on that information "
                    + "using third party services. Default value is false.");

    public static final ConfigKey GEOLOCATION_TYPE = new ConfigKey(
            "geolocation.type",
            String.class,
            "Provider to use for LBS location. Available options: google, mozilla and opencellid. By default "
                    + "opencellid is used. You have to supply a key that you get from corresponding provider. For more "
                    + "information see LBS geolocation documentation.");

    public static final ConfigKey GEOLOCATION_URL = new ConfigKey(
            "geolocation.url",
            String.class,
            "Geolocation provider URL address.");

    public static final ConfigKey GEOLOCATION_KEY = new ConfigKey(
            "geolocation.key",
            String.class,
            "Provider API key. OpenCellID service requires API key.");

    public static final ConfigKey GEOLOCATION_PROCESS_INVALID_POSITIONS = new ConfigKey(
            "geolocation.processInvalidPositions",
            Boolean.class,
            "Boolean flag to apply geolocation to invalid positions.");

    private Keys() {
    }

}
