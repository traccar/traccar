/*
 * Copyright 2019 - 2024 Anton Tananaev (anton@traccar.org)
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

import java.util.List;

public final class Keys {

    private Keys() {
    }

    /**
     * Network interface for a the protocol. If not specified, server will bind all interfaces.
     */
    public static final ConfigSuffix<String> PROTOCOL_ADDRESS = new StringConfigSuffix(
            ".address",
            List.of(KeyType.CONFIG));

    /**
     * Port number for the protocol. Most protocols use TCP on the transport layer. Some protocols use UDP. Some
     * support both TCP and UDP.
     */
    public static final ConfigSuffix<Integer> PROTOCOL_PORT = new IntegerConfigSuffix(
            ".port",
            List.of(KeyType.CONFIG));

    /**
     * List of devices for polling protocols. List should contain unique ids separated by commas. Used only for polling
     * protocols.
     */
    public static final ConfigSuffix<String> PROTOCOL_DEVICES = new StringConfigSuffix(
            ".devices",
            List.of(KeyType.CONFIG));

    /**
     * Polling interval in seconds. Used only for polling protocols.
     */
    public static final ConfigSuffix<Long> PROTOCOL_INTERVAL = new LongConfigSuffix(
            ".interval",
            List.of(KeyType.CONFIG));

    /**
     * Enable SSL support for the protocol. Not all protocols support this.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_SSL = new BooleanConfigSuffix(
            ".ssl",
            List.of(KeyType.CONFIG));

    /**
     * Connection timeout value in seconds. Because sometimes there is no way to detect lost TCP connection old
     * connections stay in open state. On most systems there is a limit on number of open connection, so this leads to
     * problems with establishing new connections when number of devices is high or devices data connections are
     * unstable.
     */
    public static final ConfigSuffix<Integer> PROTOCOL_TIMEOUT = new IntegerConfigSuffix(
            ".timeout",
            List.of(KeyType.CONFIG));

    /**
     * Device password. Commonly used in some protocol for sending commands.
     */
    public static final ConfigKey<String> DEVICE_PASSWORD = new StringConfigKey(
            "devicePassword",
            List.of(KeyType.DEVICE));

    /**
     * Device password. Commonly used in some protocol for sending commands.
     */
    public static final ConfigSuffix<String> PROTOCOL_DEVICE_PASSWORD = new StringConfigSuffix(
            ".devicePassword",
            List.of(KeyType.CONFIG));

    /**
     * Default protocol mask to use. Currently used only by Skypatrol protocol.
     */
    public static final ConfigSuffix<Integer> PROTOCOL_MASK = new IntegerConfigSuffix(
            ".mask",
            List.of(KeyType.CONFIG));

    /**
     * Custom message length. Currently used only by H2 protocol for specifying binary message length.
     */
    public static final ConfigSuffix<Integer> PROTOCOL_MESSAGE_LENGTH = new IntegerConfigSuffix(
            ".messageLength",
            List.of(KeyType.CONFIG));

    /**
     * Enable extended functionality for the protocol. The reason it's disabled by default is that not all devices
     * support it.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_EXTENDED = new BooleanConfigSuffix(
            ".extended",
            List.of(KeyType.CONFIG));

    /**
     * Decode string as UTF8 instead of ASCII. Only applicable for some protocols.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_UTF8 = new BooleanConfigSuffix(
            ".utf8",
            List.of(KeyType.CONFIG));

    /**
     * Enable CAN decoding for the protocol. Similar to 'extended' configuration, it's not supported for some devices.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_CAN = new BooleanConfigSuffix(
            ".can",
            List.of(KeyType.CONFIG));

    /**
     * Indicates whether server acknowledgement is required. Only applicable for some protocols.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_ACK = new BooleanConfigSuffix(
            ".ack",
            List.of(KeyType.CONFIG, KeyType.DEVICE),
            false);

    /**
     * Ignore device reported fix time. Useful in case some devices report invalid time. Currently only available for
     * GL200 protocol.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_IGNORE_FIX_TIME = new BooleanConfigSuffix(
            ".ignoreFixTime",
            List.of(KeyType.CONFIG));

    /**
     * Decode additional TK103 attributes. Not supported for some devices.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_DECODE_LOW = new BooleanConfigSuffix(
            ".decodeLow",
            List.of(KeyType.CONFIG));

    /**
     * Use long date format for Atrack protocol.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_LONG_DATE = new BooleanConfigSuffix(
            ".longDate",
            List.of(KeyType.CONFIG));

    /**
     * Use decimal fuel value format for Atrack protocol.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_DECIMAL_FUEL = new BooleanConfigSuffix(
            ".decimalFuel",
            List.of(KeyType.CONFIG));

    /**
     * Indicates additional custom attributes for Atrack protocol.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_CUSTOM = new BooleanConfigSuffix(
            ".custom",
            List.of(KeyType.CONFIG));

    /**
     * Custom format string for Atrack protocol.
     */
    public static final ConfigSuffix<String> PROTOCOL_FORM = new StringConfigSuffix(
            ".form",
            List.of(KeyType.CONFIG));

    /**
     * Protocol configuration. Required for some devices for decoding incoming data.
     */
    public static final ConfigSuffix<String> PROTOCOL_CONFIG = new StringConfigSuffix(
            ".config",
            List.of(KeyType.CONFIG));

    /**
     * Alarm mapping for Atrack protocol.
     */
    public static final ConfigSuffix<String> PROTOCOL_ALARM_MAP = new StringConfigSuffix(
            ".alarmMap",
            List.of(KeyType.CONFIG));

    /**
     * Indicates whether TAIP protocol should have prefixes for messages.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_PREFIX = new BooleanConfigSuffix(
            ".prefix",
            List.of(KeyType.CONFIG));

    /**
     * Some devices require server address confirmation. Use this parameter to configure correct public address.
     */
    public static final ConfigSuffix<String> PROTOCOL_SERVER = new StringConfigSuffix(
            ".server",
            List.of(KeyType.CONFIG));

    /**
     * Protocol type for Suntech.
     */
    public static final ConfigKey<Integer> PROTOCOL_TYPE = new IntegerConfigKey(
            "suntech.protocolType",
            List.of(KeyType.CONFIG, KeyType.DEVICE));

    /**
     * Suntech HBM configuration value.
     */
    public static final ConfigKey<Boolean> PROTOCOL_HBM = new BooleanConfigKey(
            "suntech.hbm",
            List.of(KeyType.CONFIG, KeyType.DEVICE));

    /**
     * Format includes ADC value.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_INCLUDE_ADC = new BooleanConfigSuffix(
            ".includeAdc",
            List.of(KeyType.CONFIG, KeyType.DEVICE));

    /**
     * Format includes RPM value.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_INCLUDE_RPM = new BooleanConfigSuffix(
            ".includeRpm",
            List.of(KeyType.CONFIG, KeyType.DEVICE));

    /**
     * Format includes temperature values.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_INCLUDE_TEMPERATURE = new BooleanConfigSuffix(
            ".includeTemp",
            List.of(KeyType.CONFIG, KeyType.DEVICE));

    /**
     * Disable commands for the protocol. Not all protocols support this option.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_DISABLE_COMMANDS = new BooleanConfigSuffix(
            ".disableCommands",
            List.of(KeyType.CONFIG));

    /**
     * Protocol format. Used by protocols that have configurable message format.
     */
    public static final ConfigSuffix<String> PROTOCOL_FORMAT = new StringConfigSuffix(
            ".format",
            List.of(KeyType.CONFIG, KeyType.DEVICE));

    /**
     * Protocol date format. Used by protocols that have configurable date format.
     */
    public static final ConfigSuffix<String> PROTOCOL_DATE_FORMAT = new StringConfigSuffix(
            ".dateFormat",
            List.of(KeyType.DEVICE));

    /**
     * Device time zone. Most devices report UTC time, but in some cases devices report local time, so this parameter
     * needs to be configured for the server to be able to decode the time correctly.
     */
    public static final ConfigKey<String> DECODER_TIMEZONE = new StringConfigKey(
            "decoder.timezone",
            List.of(KeyType.CONFIG, KeyType.DEVICE));

    /**
     * ORBCOMM API access id.
     */
    public static final ConfigKey<String> ORBCOMM_ACCESS_ID = new StringConfigKey(
            "orbcomm.accessId",
            List.of(KeyType.CONFIG));

    /**
     * ORBCOMM API password.
     */
    public static final ConfigKey<String> ORBCOMM_PASSWORD = new StringConfigKey(
            "orbcomm.password",
            List.of(KeyType.CONFIG));

    /**
     * Use alternative format for the protocol of commands.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_ALTERNATIVE = new BooleanConfigSuffix(
            ".alternative",
            List.of(KeyType.CONFIG, KeyType.DEVICE),
            false);

    /**
     * Protocol format includes a language field.
     */
    public static final ConfigSuffix<Boolean> PROTOCOL_LANGUAGE = new BooleanConfigSuffix(
            ".language",
            List.of(KeyType.CONFIG, KeyType.DEVICE),
            false);

    /**
     * If not zero, enable buffering of incoming data to handle ordering locations. The value is threshold for
     * buffering in milliseconds.
     */
    public static final ConfigKey<Long> SERVER_BUFFERING_THRESHOLD = new LongConfigKey(
            "server.buffering.threshold",
            List.of(KeyType.CONFIG));

    /**
     * Server wide connection timeout value in seconds. See protocol timeout for more information.
     */
    public static final ConfigKey<Integer> SERVER_TIMEOUT = new IntegerConfigKey(
            "server.timeout",
            List.of(KeyType.CONFIG));

    /**
     * Send device responses immediately before writing it in the database.
     */
    public static final ConfigKey<Boolean> SERVER_INSTANT_ACKNOWLEDGEMENT = new BooleanConfigKey(
            "server.instantAcknowledgement",
            List.of(KeyType.CONFIG));

    /**
     * Address for uploading aggregated anonymous usage statistics. Uploaded information is the same you can see on the
     * statistics screen in the web app. It does not include any sensitive (e.g. locations).
     */
    public static final ConfigKey<String> SERVER_STATISTICS = new StringConfigKey(
            "server.statistics",
            List.of(KeyType.CONFIG));

    /**
     * Fuel drop threshold value. When fuel level drops from one position to another for more the value, an event is
     * generated.
     */
    public static final ConfigKey<Double> EVENT_FUEL_DROP_THRESHOLD = new DoubleConfigKey(
            "fuelDropThreshold",
            List.of(KeyType.SERVER, KeyType.DEVICE),
            0.0);

    /**
     * Fuel increase threshold value. When fuel level increases from one position to another for more the value, an
     * event is generated.
     */
    public static final ConfigKey<Double> EVENT_FUEL_INCREASE_THRESHOLD = new DoubleConfigKey(
            "fuelIncreaseThreshold",
            List.of(KeyType.SERVER, KeyType.DEVICE),
            0.0);

    /**
     * Speed limit value in knots.
     */
    public static final ConfigKey<Double> EVENT_OVERSPEED_LIMIT = new DoubleConfigKey(
            "speedLimit",
            List.of(KeyType.SERVER, KeyType.DEVICE),
            0.0);

    /**
     * Disable device sharing on the server.
     */
    public static final ConfigKey<Boolean> DEVICE_SHARE_DISABLE = new BooleanConfigKey(
            "disableShare",
            List.of(KeyType.SERVER));

    /**
     * Speed limit threshold multiplier. For example, if the speed limit is 100, but we only want to generate an event
     * if the speed is higher than 105, this parameter can be set to 1.05. Default multiplier is 1.0.
     */
    public static final ConfigKey<Double> EVENT_OVERSPEED_THRESHOLD_MULTIPLIER = new DoubleConfigKey(
            "event.overspeed.thresholdMultiplier",
            List.of(KeyType.CONFIG),
            1.0);

    /**
     * Minimal over speed duration to trigger the event. Value in seconds.
     */
    public static final ConfigKey<Long> EVENT_OVERSPEED_MINIMAL_DURATION = new LongConfigKey(
            "event.overspeed.minimalDuration",
            List.of(KeyType.CONFIG));

    /**
     * Relevant only for geofence speed limits. Use the lowest speed limit from all geofences.
     */
    public static final ConfigKey<Boolean> EVENT_OVERSPEED_PREFER_LOWEST = new BooleanConfigKey(
            "event.overspeed.preferLowest",
            List.of(KeyType.CONFIG));

    /**
     * Driver behavior acceleration threshold. Value is in meter per second squared.
     */
    public static final ConfigKey<Double> EVENT_BEHAVIOR_ACCELERATION_THRESHOLD = new DoubleConfigKey(
            "event.behavior.accelerationThreshold",
            List.of(KeyType.CONFIG));

    /**
     * Driver behavior braking threshold. Value is in meter per second squared.
     */
    public static final ConfigKey<Double> EVENT_BEHAVIOR_BRAKING_THRESHOLD = new DoubleConfigKey(
            "event.behavior.brakingThreshold",
            List.of(KeyType.CONFIG));

    /**
     * Do not generate alert event if same alert was present in last known location.
     */
    public static final ConfigKey<Boolean> EVENT_IGNORE_DUPLICATE_ALERTS = new BooleanConfigKey(
            "event.ignoreDuplicateAlerts",
            List.of(KeyType.CONFIG));

    /**
     * If set to true, invalid positions will be considered for motion logic.
     */
    public static final ConfigKey<Boolean> EVENT_MOTION_PROCESS_INVALID_POSITIONS = new BooleanConfigKey(
            "event.motion.processInvalidPositions",
            List.of(KeyType.CONFIG, KeyType.DEVICE),
            false);

    /**
     * If the speed is above specified value, the object is considered to be in motion. Default value is 0.01 knots.
     */
    public static final ConfigKey<Double> EVENT_MOTION_SPEED_THRESHOLD = new DoubleConfigKey(
            "event.motion.speedThreshold",
            List.of(KeyType.CONFIG, KeyType.DEVICE),
            0.01);

    /**
     * Global polyline geofence distance. Within that distance from the polyline, point is considered within the
     * geofence. Each individual geofence can also has 'polylineDistance' attribute which will take precedence.
     */
    public static final ConfigKey<Double> GEOFENCE_POLYLINE_DISTANCE = new DoubleConfigKey(
            "geofence.polylineDistance",
            List.of(KeyType.CONFIG),
            25.0);

    /**
     * Enable in-memory database instead of an SQL database.
     */
    public static final ConfigKey<Boolean> DATABASE_MEMORY = new BooleanConfigKey(
            "database.memory",
            List.of(KeyType.CONFIG));

    /**
     * Path to the database driver JAR file. Traccar includes drivers for MySQL, PostgreSQL and H2 databases. If you use
     * one of those, you don't need to specify this parameter.
     */
    public static final ConfigKey<String> DATABASE_DRIVER_FILE = new StringConfigKey(
            "database.driverFile",
            List.of(KeyType.CONFIG));

    /**
     * Database driver Java class. For H2 use 'org.h2.Driver'. MySQL driver class name is 'com.mysql.jdbc.Driver'.
     */
    public static final ConfigKey<String> DATABASE_DRIVER = new StringConfigKey(
            "database.driver",
            List.of(KeyType.CONFIG));

    /**
     * Database connection URL. By default Traccar uses H2 database.
     */
    public static final ConfigKey<String> DATABASE_URL = new StringConfigKey(
            "database.url",
            List.of(KeyType.CONFIG));

    /**
     * Database user name. Default administrator user for H2 database is 'sa'.
     */
    public static final ConfigKey<String> DATABASE_USER = new StringConfigKey(
            "database.user",
            List.of(KeyType.CONFIG));

    /**
     * Database user password. Default password for H2 admin (sa) user is empty.
     */
    public static final ConfigKey<String> DATABASE_PASSWORD = new StringConfigKey(
            "database.password",
            List.of(KeyType.CONFIG));

    /**
     * Path to Liquibase master changelog file.
     */
    public static final ConfigKey<String> DATABASE_CHANGELOG = new StringConfigKey(
            "database.changelog",
            List.of(KeyType.CONFIG));

    /**
     * Database connection pool size. Default value is defined by the HikariCP library.
     */
    public static final ConfigKey<Integer> DATABASE_MAX_POOL_SIZE = new IntegerConfigKey(
            "database.maxPoolSize",
            List.of(KeyType.CONFIG));

    /**
     * SQL query to check connection status. Default value is 'SELECT 1'. For Oracle database you can use
     * 'SELECT 1 FROM DUAL'.
     */
    public static final ConfigKey<String> DATABASE_CHECK_CONNECTION = new StringConfigKey(
            "database.checkConnection",
            List.of(KeyType.CONFIG),
            "SELECT 1");

    /**
     * Store original HEX or string data as "raw" attribute in the corresponding position.
     */
    public static final ConfigKey<Boolean> DATABASE_SAVE_ORIGINAL = new BooleanConfigKey(
            "database.saveOriginal",
            List.of(KeyType.CONFIG));

    /**
     * Throttle unknown device database queries when it sends repeated requests.
     */
    public static final ConfigKey<Boolean> DATABASE_THROTTLE_UNKNOWN = new BooleanConfigKey(
            "database.throttleUnknown",
            List.of(KeyType.CONFIG));

    /**
     * Automatically register unknown devices in the database.
     */
    public static final ConfigKey<Boolean> DATABASE_REGISTER_UNKNOWN = new BooleanConfigKey(
            "database.registerUnknown",
            List.of(KeyType.CONFIG));

    /**
     * Default category for auto-registered devices.
     */
    public static final ConfigKey<String> DATABASE_REGISTER_UNKNOWN_DEFAULT_CATEGORY = new StringConfigKey(
            "database.registerUnknown.defaultCategory",
            List.of(KeyType.CONFIG));

    /**
     * The group id assigned to auto-registered devices.
     */
    public static final ConfigKey<Long> DATABASE_REGISTER_UNKNOWN_DEFAULT_GROUP_ID = new LongConfigKey(
            "database.registerUnknown.defaultGroupId",
            List.of(KeyType.CONFIG));

    /**
     * Automatically register unknown devices with regex filter.
     */
    public static final ConfigKey<String> DATABASE_REGISTER_UNKNOWN_REGEX = new StringConfigKey(
            "database.registerUnknown.regex",
            List.of(KeyType.CONFIG), "\\w{3,15}");

    /**
     * Store empty messages as positions. For example, heartbeats.
     */
    public static final ConfigKey<Boolean> DATABASE_SAVE_EMPTY = new BooleanConfigKey(
            "database.saveEmpty",
            List.of(KeyType.CONFIG));

    /**
     * Device limit for self registered users. Default value is -1, which indicates no limit.
     */
    public static final ConfigKey<Integer> USERS_DEFAULT_DEVICE_LIMIT = new IntegerConfigKey(
            "users.defaultDeviceLimit",
            List.of(KeyType.CONFIG),
            -1);

    /**
     * Default user expiration for self registered users. Value is in days. By default no expiration is set.
     */
    public static final ConfigKey<Integer> USERS_DEFAULT_EXPIRATION_DAYS = new IntegerConfigKey(
            "users.defaultExpirationDays",
            List.of(KeyType.CONFIG));

    /**
     * LDAP server URL. For more info check <a href="https://www.traccar.org/ldap/">LDAP config</a>.
     */
    public static final ConfigKey<String> LDAP_URL = new StringConfigKey(
            "ldap.url",
            List.of(KeyType.CONFIG));

    /**
     * LDAP server login.
     */
    public static final ConfigKey<String> LDAP_USER = new StringConfigKey(
            "ldap.user",
            List.of(KeyType.CONFIG));

    /**
     * LDAP server password.
     */
    public static final ConfigKey<String> LDAP_PASSWORD = new StringConfigKey(
            "ldap.password",
            List.of(KeyType.CONFIG));

    /**
     * Force LDAP authentication.
     */
    public static final ConfigKey<Boolean> LDAP_FORCE = new BooleanConfigKey(
            "ldap.force",
            List.of(KeyType.CONFIG));

    /**
     * LDAP user search base.
     */
    public static final ConfigKey<String> LDAP_BASE = new StringConfigKey(
            "ldap.base",
            List.of(KeyType.CONFIG));

    /**
     * LDAP attribute used as user id. Default value is 'uid'.
     */
    public static final ConfigKey<String> LDAP_ID_ATTRIBUTE = new StringConfigKey(
            "ldap.idAttribute",
            List.of(KeyType.CONFIG),
            "uid");

    /**
     * LDAP attribute used as user name. Default value is 'cn'.
     */
    public static final ConfigKey<String> LDAP_NAME_ATTRIBUTE = new StringConfigKey(
            "ldap.nameAttribute",
            List.of(KeyType.CONFIG),
            "cn");

    /**
     * LDAP attribute used as user email. Default value is 'mail'.
     */
    public static final ConfigKey<String> LDAP_MAIN_ATTRIBUTE = new StringConfigKey(
            "ldap.mailAttribute",
            List.of(KeyType.CONFIG),
            "mail");

    /**
     * LDAP custom search filter. If not specified, '({idAttribute}=:login)' will be used as a filter.
     */
    public static final ConfigKey<String> LDAP_SEARCH_FILTER = new StringConfigKey(
            "ldap.searchFilter",
            List.of(KeyType.CONFIG));

    /**
     * LDAP custom admin search filter.
     */
    public static final ConfigKey<String> LDAP_ADMIN_FILTER = new StringConfigKey(
            "ldap.adminFilter",
            List.of(KeyType.CONFIG));

    /**
     * LDAP admin user group. Used if custom admin filter is not specified.
     */
    public static final ConfigKey<String> LDAP_ADMIN_GROUP = new StringConfigKey(
            "ldap.adminGroup",
            List.of(KeyType.CONFIG));

    /**
     * Force OpenID Connect authentication. When enabled, the Traccar login page will be skipped
     * and users are redirected to the OpenID Connect provider.
     */
    public static final ConfigKey<Boolean> OPENID_FORCE = new BooleanConfigKey(
            "openid.force",
            List.of(KeyType.CONFIG));

    /**
     * OpenID Connect Client ID.
     * This is a unique ID assigned to each application you register with your identity provider.
     * Required to enable SSO.
     */
    public static final ConfigKey<String> OPENID_CLIENT_ID = new StringConfigKey(
            "openid.clientId",
            List.of(KeyType.CONFIG));

    /**
     * OpenID Connect Client Secret.
     * This is a secret assigned to each application you register with your identity provider.
     * Required to enable SSO.
     */
    public static final ConfigKey<String> OPENID_CLIENT_SECRET = new StringConfigKey(
            "openid.clientSecret",
            List.of(KeyType.CONFIG));

    /**
     * OpenID Connect Issuer (Base) URL.
     * This is used to automatically configure the authorization, token and user info URLs if provided.
     */
    public static final ConfigKey<String> OPENID_ISSUER_URL = new StringConfigKey(
            "openid.issuerUrl",
            List.of(KeyType.CONFIG));

    /**
     * OpenID Connect Authorization URL.
     * This can usually be found in the documentation of your identity provider or by using the well-known
     * configuration endpoint, e.g. https://auth.example.com//.well-known/openid-configuration
     * Required to enable SSO if openid.issuerUrl is not set.
     */
    public static final ConfigKey<String> OPENID_AUTH_URL = new StringConfigKey(
            "openid.authUrl",
            List.of(KeyType.CONFIG));
    /**
     * OpenID Connect Token URL.
     * This can be found in the same ways at openid.authUrl.
     * Required to enable SSO if openid.issuerUrl is not set.
     */
    public static final ConfigKey<String> OPENID_TOKEN_URL = new StringConfigKey(
            "openid.tokenUrl",
            List.of(KeyType.CONFIG));

    /**
     * OpenID Connect User Info URL.
     * This can be found in the same ways at openid.authUrl.
     * Required to enable SSO if openid.issuerUrl is not set.
     */
    public static final ConfigKey<String> OPENID_USERINFO_URL = new StringConfigKey(
            "openid.userInfoUrl",
            List.of(KeyType.CONFIG));

    /**
     * OpenID Connect group to restrict access to.
     * If this is not provided, all OpenID users will have access to Traccar.
     * This option will only work if your OpenID provider supports the groups scope.
     */
    public static final ConfigKey<String> OPENID_ALLOW_GROUP = new StringConfigKey(
        "openid.allowGroup",
        List.of(KeyType.CONFIG));

    /**
     * OpenID Connect group to grant admin access.
     * If this is not provided, no groups will be granted admin access.
     * This option will only work if your OpenID provider supports the groups scope.
     */
    public static final ConfigKey<String> OPENID_ADMIN_GROUP = new StringConfigKey(
            "openid.adminGroup",
            List.of(KeyType.CONFIG));

    /**
     * If no data is reported by a device for the given amount of time, status changes from online to unknown. Value is
     * in seconds. Default timeout is 10 minutes.
     */
    public static final ConfigKey<Long> STATUS_TIMEOUT = new LongConfigKey(
            "status.timeout",
            List.of(KeyType.CONFIG),
            600L);

    /**
     * List of protocol names to ignore offline status. Can be useful to not trigger status change when devices are
     * configured to disconnect after reporting a batch of data.
     */
    public static final ConfigKey<String> STATUS_IGNORE_OFFLINE = new StringConfigKey(
            "status.ignoreOffline",
            List.of(KeyType.CONFIG));

    /**
     * Path to the media folder. Server stores audio, video and photo files in that folder. Sub-folders will be
     * automatically created for each device by unique id.
     */
    public static final ConfigKey<String> MEDIA_PATH = new StringConfigKey(
            "media.path",
            List.of(KeyType.CONFIG));

    /**
     * Optional parameter to specify network interface for web interface to bind to. By default server will bind to all
     * available interfaces.
     */
    public static final ConfigKey<String> WEB_ADDRESS = new StringConfigKey(
            "web.address",
            List.of(KeyType.CONFIG));

    /**
     * Web interface TCP port number. By default Traccar uses port 8082. To avoid specifying port in the browser you
     * can set it to 80 (default HTTP port).
     */
    public static final ConfigKey<Integer> WEB_PORT = new IntegerConfigKey(
            "web.port",
            List.of(KeyType.CONFIG));

    /**
     * Maximum API requests per second. Above this limit requests and delayed and throttled.
     */
    public static final ConfigKey<Integer> WEB_MAX_REQUESTS_PER_SECOND = new IntegerConfigKey(
            "web.maxRequestsPerSec",
            List.of(KeyType.CONFIG));

    /**
     * Maximum API request duration in seconds.
     */
    public static final ConfigKey<Integer> WEB_MAX_REQUEST_SECONDS = new IntegerConfigKey(
            "web.maxRequestSec",
            List.of(KeyType.CONFIG),
            600);

    /**
     * Sanitize all strings returned via API. This is needed to fix XSS issues in the old web interface. New React-based
     * interface doesn't require this.
     */
    public static final ConfigKey<Boolean> WEB_SANITIZE = new BooleanConfigKey(
            "web.sanitize",
            List.of(KeyType.CONFIG));

    /**
     * Path to the web app folder.
     */
    public static final ConfigKey<String> WEB_PATH = new StringConfigKey(
            "web.path",
            List.of(KeyType.CONFIG));

    /**
     * Path to a folder with overrides. It can be used for branding to keep custom logos in a separate place.
     */
    public static final ConfigKey<String> WEB_OVERRIDE = new StringConfigKey(
            "web.override",
            List.of(KeyType.CONFIG));

    /**
     * WebSocket connection timeout in milliseconds. Default timeout is 5 minutes.
     */
    public static final ConfigKey<Long> WEB_TIMEOUT = new LongConfigKey(
            "web.timeout",
            List.of(KeyType.CONFIG),
            300000L);

    /**
     * Authentication sessions timeout in seconds. By default no timeout.
     */
    public static final ConfigKey<Integer> WEB_SESSION_TIMEOUT = new IntegerConfigKey(
            "web.sessionTimeout",
            List.of(KeyType.CONFIG));

    /**
     * Enable database access console via '/console' URL. Use only for debugging. Never use in production.
     */
    public static final ConfigKey<Boolean> WEB_CONSOLE = new BooleanConfigKey(
            "web.console",
            List.of(KeyType.CONFIG));

    /**
     * Server debug version of the web app. Not recommended to use for performance reasons. It is intended to be used
     * for development and debugging purposes.
     */
    public static final ConfigKey<Boolean> WEB_DEBUG = new BooleanConfigKey(
            "web.debug",
            List.of(KeyType.CONFIG));

    /**
     * A token to login as a virtual admin account. Can be used to restore access in case of issues with regular admin
     * login. For example, if password is lost and can't be restored.
     */
    public static final ConfigKey<String> WEB_SERVICE_ACCOUNT_TOKEN = new StringConfigKey(
            "web.serviceAccountToken",
            List.of(KeyType.CONFIG));

    /**
     * Cross-origin resource sharing origin header value.
     */
    public static final ConfigKey<String> WEB_ORIGIN = new StringConfigKey(
            "web.origin",
            List.of(KeyType.CONFIG));

    /**
     * Cache control header value. By default resources are cached for one hour.
     */
    public static final ConfigKey<String> WEB_CACHE_CONTROL = new StringConfigKey(
            "web.cacheControl",
            List.of(KeyType.CONFIG),
            "max-age=3600,public");

    /**
     * Enable TOTP authentication on the server.
     */
    public static final ConfigKey<Boolean> WEB_TOTP_ENABLE = new BooleanConfigKey(
            "totpEnable",
            List.of(KeyType.SERVER));

    /**
     * Server attribute that indicates that TOTP authentication is required for new users.
     */
    public static final ConfigKey<Boolean> WEB_TOTP_FORCE = new BooleanConfigKey(
            "totpForce",
            List.of(KeyType.SERVER));

    /**
     * Host for raw data forwarding.
     */
    public static final ConfigKey<String> SERVER_FORWARD = new StringConfigKey(
            "server.forward",
            List.of(KeyType.CONFIG));

    /**
     * Position forwarding format. Available options are "url", "json" and "kafka". Default is "url".
     */
    public static final ConfigKey<String> FORWARD_TYPE = new StringConfigKey(
            "forward.type",
            List.of(KeyType.CONFIG),
            "url");

    /**
     * Position forwarding AMQP exchange.
     */
    public static final ConfigKey<String> FORWARD_EXCHANGE = new StringConfigKey(
            "forward.exchange",
            List.of(KeyType.CONFIG),
            "traccar");

    /**
     * Position forwarding Kafka topic or AQMP Routing Key.
     */
    public static final ConfigKey<String> FORWARD_TOPIC = new StringConfigKey(
            "forward.topic",
            List.of(KeyType.CONFIG),
            "positions");

    /**
     * URL to forward positions. Data is passed through URL parameters. For example, {uniqueId} for device identifier,
     * {latitude} and {longitude} for coordinates.
     */
    public static final ConfigKey<String> FORWARD_URL = new StringConfigKey(
            "forward.url",
            List.of(KeyType.CONFIG));

    /**
     * Additional HTTP header, can be used for authorization.
     */
    public static final ConfigKey<String> FORWARD_HEADER = new StringConfigKey(
            "forward.header",
            List.of(KeyType.CONFIG));

    /**
     * Position forwarding retrying enable. When enabled, additional attempts are made to deliver positions. If initial
     * delivery fails, because of an unreachable server or an HTTP response different from '2xx', the software waits
     * for 'forward.retry.delay' milliseconds to retry delivery. On subsequent failures, this delay is duplicated.
     * If forwarding is retried for 'forward.retry.count', retrying is canceled and the position is dropped. Positions
     * pending to be delivered are limited to 'forward.retry.limit'. If this limit is reached, positions get discarded.
     */
    public static final ConfigKey<Boolean> FORWARD_RETRY_ENABLE = new BooleanConfigKey(
            "forward.retry.enable",
            List.of(KeyType.CONFIG));

    /**
     * Position forwarding retry first delay in milliseconds.
     * Can be set to anything greater than 0. Defaults to 100 milliseconds.
     */
    public static final ConfigKey<Integer> FORWARD_RETRY_DELAY = new IntegerConfigKey(
            "forward.retry.delay",
            List.of(KeyType.CONFIG),
            100);

    /**
     * Position forwarding retry maximum retries.
     * Can be set to anything greater than 0. Defaults to 10 retries.
     */
    public static final ConfigKey<Integer> FORWARD_RETRY_COUNT = new IntegerConfigKey(
            "forward.retry.count",
            List.of(KeyType.CONFIG),
            10);

    /**
     * Position forwarding retry pending positions limit.
     * Can be set to anything greater than 0. Defaults to 100 positions.
     */
    public static final ConfigKey<Integer> FORWARD_RETRY_LIMIT = new IntegerConfigKey(
            "forward.retry.limit",
            List.of(KeyType.CONFIG),
            100);

    /**
     * Events forwarding format. Available options are "json" and "kafka". Default is "json".
     */
    public static final ConfigKey<String> EVENT_FORWARD_TYPE = new StringConfigKey(
            "event.forward.type",
            List.of(KeyType.CONFIG),
            "json");

    /**
     * Events forwarding AMQP exchange.
     */
    public static final ConfigKey<String> EVENT_FORWARD_EXCHANGE = new StringConfigKey(
            "event.forward.exchange",
            List.of(KeyType.CONFIG),
            "traccar");

    /**
     * Events forwarding Kafka topic or AQMP Routing Key.
     */
    public static final ConfigKey<String> EVENT_FORWARD_TOPIC = new StringConfigKey(
            "event.forward.topic",
            List.of(KeyType.CONFIG),
            "events");

    /**
     * Events forwarding URL.
     */
    public static final ConfigKey<String> EVENT_FORWARD_URL = new StringConfigKey(
            "event.forward.url",
            List.of(KeyType.CONFIG));

    /**
     * Events forwarding headers. Example value:
     * FirstHeader: hello
     * SecondHeader: world
     */
    public static final ConfigKey<String> EVENT_FORWARD_HEADERS = new StringConfigKey(
            "event.forward.header",
            List.of(KeyType.CONFIG));

    /**
     * Root folder for all template files.
     */
    public static final ConfigKey<String> TEMPLATES_ROOT = new StringConfigKey(
            "templates.root",
            List.of(KeyType.CONFIG),
            "templates");

    /**
     * Log emails instead of sending them via SMTP. Intended for testing purposes only.
     */
    public static final ConfigKey<Boolean> MAIL_DEBUG = new BooleanConfigKey(
            "mail.debug",
            List.of(KeyType.CONFIG));

    /**
     * Restrict global SMTP configuration to system messages only (e.g. password reset).
     */
    public static final ConfigKey<Boolean> MAIL_SMTP_SYSTEM_ONLY = new BooleanConfigKey(
            "mail.smtp.systemOnly",
            List.of(KeyType.CONFIG));

    /**
     * Force SMTP settings from the config file and ignore user attributes.
     */
    public static final ConfigKey<Boolean> MAIL_SMTP_IGNORE_USER_CONFIG = new BooleanConfigKey(
            "mail.smtp.ignoreUserConfig",
            List.of(KeyType.CONFIG));

    /**
     * The SMTP server to connect to.
     */
    public static final ConfigKey<String> MAIL_SMTP_HOST = new StringConfigKey(
            "mail.smtp.host",
            List.of(KeyType.CONFIG, KeyType.USER));

    /**
     * The SMTP server port to connect. Defaults to 25.
     */
    public static final ConfigKey<Integer> MAIL_SMTP_PORT = new IntegerConfigKey(
            "mail.smtp.port",
            List.of(KeyType.CONFIG, KeyType.USER),
            25);

    /**
     * Email transport protocol. Default value is "smtp".
     */
    public static final ConfigKey<String> MAIL_TRANSPORT_PROTOCOL = new StringConfigKey(
            "mail.transport.protocol",
            List.of(KeyType.CONFIG, KeyType.USER),
            "smtp");

    /**
     * If true, enables the use of the STARTTLS command (if supported by the server) to switch the connection to a
     * TLS-protected connection before issuing any login commands.
     */
    public static final ConfigKey<Boolean> MAIL_SMTP_STARTTLS_ENABLE = new BooleanConfigKey(
            "mail.smtp.starttls.enable",
            List.of(KeyType.CONFIG, KeyType.USER));

    /**
     * If true, requires the use of the STARTTLS command. If the server doesn't support the STARTTLS command, or the
     * command fails, the connect method will fail.
     */
    public static final ConfigKey<Boolean> MAIL_SMTP_STARTTLS_REQUIRED = new BooleanConfigKey(
            "mail.smtp.starttls.required",
            List.of(KeyType.CONFIG, KeyType.USER));

    /**
     * If set to true, use SSL to connect and use the SSL port by default.
     */
    public static final ConfigKey<Boolean> MAIL_SMTP_SSL_ENABLE = new BooleanConfigKey(
            "mail.smtp.ssl.enable",
            List.of(KeyType.CONFIG, KeyType.USER));

    /**
     * If set to "*", all hosts are trusted. If set to a whitespace separated list of hosts, those hosts are trusted.
     * Otherwise, trust depends on the certificate the server presents.
     */
    public static final ConfigKey<String> MAIL_SMTP_SSL_TRUST = new StringConfigKey(
            "mail.smtp.ssl.trust",
            List.of(KeyType.CONFIG, KeyType.USER));

    /**
     * Specifies the SSL protocols that will be enabled for SSL connections.
     */
    public static final ConfigKey<String> MAIL_SMTP_SSL_PROTOCOLS = new StringConfigKey(
            "mail.smtp.ssl.protocols",
            List.of(KeyType.CONFIG, KeyType.USER));

    /**
     * SMTP connection username.
     */
    public static final ConfigKey<String> MAIL_SMTP_USERNAME = new StringConfigKey(
            "mail.smtp.username",
            List.of(KeyType.CONFIG, KeyType.USER));

    /**
     * SMTP connection password.
     */
    public static final ConfigKey<String> MAIL_SMTP_PASSWORD = new StringConfigKey(
            "mail.smtp.password",
            List.of(KeyType.CONFIG, KeyType.USER));

    /**
     * Email address to use for SMTP MAIL command.
     */
    public static final ConfigKey<String> MAIL_SMTP_FROM = new StringConfigKey(
            "mail.smtp.from",
            List.of(KeyType.CONFIG, KeyType.USER));

    /**
     * The personal name for the email from address.
     */
    public static final ConfigKey<String> MAIL_SMTP_FROM_NAME = new StringConfigKey(
            "mail.smtp.fromName",
            List.of(KeyType.CONFIG, KeyType.USER));

    /**
     * SMS API service full URL. Enables SMS commands and notifications.
     */
    public static final ConfigKey<String> SMS_HTTP_URL = new StringConfigKey(
            "sms.http.url",
            List.of(KeyType.CONFIG));

    /**
     * SMS API authorization header name. Default value is 'Authorization'.
     */
    public static final ConfigKey<String> SMS_HTTP_AUTHORIZATION_HEADER = new StringConfigKey(
            "sms.http.authorizationHeader",
            List.of(KeyType.CONFIG),
            "Authorization");

    /**
     * SMS API authorization header value. This value takes precedence over user and password.
     */
    public static final ConfigKey<String> SMS_HTTP_AUTHORIZATION = new StringConfigKey(
            "sms.http.authorization",
            List.of(KeyType.CONFIG));

    /**
     * SMS API basic authentication user.
     */
    public static final ConfigKey<String> SMS_HTTP_USER = new StringConfigKey(
            "sms.http.user",
            List.of(KeyType.CONFIG));

    /**
     * SMS API basic authentication password.
     */
    public static final ConfigKey<String> SMS_HTTP_PASSWORD = new StringConfigKey(
            "sms.http.password",
            List.of(KeyType.CONFIG));

    /**
     * SMS API body template. Placeholders {phone} and {message} can be used in the template.
     * If value starts with '{' or '[', server automatically assumes JSON format.
     */
    public static final ConfigKey<String> SMS_HTTP_TEMPLATE = new StringConfigKey(
            "sms.http.template",
            List.of(KeyType.CONFIG));

    /**
     * AWS Access Key with SNS permission.
     */
    public static final ConfigKey<String> SMS_AWS_ACCESS = new StringConfigKey(
            "sms.aws.access",
            List.of(KeyType.CONFIG));

    /**
     * AWS Secret Access Key with SNS permission.
     */
    public static final ConfigKey<String> SMS_AWS_SECRET = new StringConfigKey(
            "sms.aws.secret",
            List.of(KeyType.CONFIG));

    /**
     * AWS Region for SNS service.
     * Make sure to use regions that are supported for messaging.
     */
    public static final ConfigKey<String> SMS_AWS_REGION = new StringConfigKey(
            "sms.aws.region",
            List.of(KeyType.CONFIG));

    /**
     * Enabled notification options. Comma-separated string is expected.
     * Example: web,mail,sms
     */
    public static final ConfigKey<String> NOTIFICATOR_TYPES = new StringConfigKey(
            "notificator.types",
            List.of(KeyType.CONFIG));

    /**
     * If the event time is too old, we should not send notifications. This parameter is the threshold value in
     * milliseconds. Default value is 15 minutes.
     */
    public static final ConfigKey<Long> NOTIFICATOR_TIME_THRESHOLD = new LongConfigKey(
            "notificator.timeThreshold",
            List.of(KeyType.CONFIG),
            15 * 60 * 1000L);

    /**
     * Traccar notification API key.
     */
    public static final ConfigKey<String> NOTIFICATOR_TRACCAR_KEY = new StringConfigKey(
            "notificator.traccar.key",
            List.of(KeyType.CONFIG));

    /**
     * Firebase service account JSON.
     */
    public static final ConfigKey<String> NOTIFICATOR_FIREBASE_SERVICE_ACCOUNT = new StringConfigKey(
            "notificator.firebase.serviceAccount",
            List.of(KeyType.CONFIG));

    /**
     * Pushover notification user name.
     */
    public static final ConfigKey<String> NOTIFICATOR_PUSHOVER_USER = new StringConfigKey(
            "notificator.pushover.user",
            List.of(KeyType.CONFIG));

    /**
     * Pushover notification user token.
     */
    public static final ConfigKey<String> NOTIFICATOR_PUSHOVER_TOKEN = new StringConfigKey(
            "notificator.pushover.token",
            List.of(KeyType.CONFIG));

    /**
     * Telegram notification API key.
     */
    public static final ConfigKey<String> NOTIFICATOR_TELEGRAM_KEY = new StringConfigKey(
            "notificator.telegram.key",
            List.of(KeyType.CONFIG));

    /**
     * Telegram notification chat id to post messages to.
     */
    public static final ConfigKey<String> NOTIFICATOR_TELEGRAM_CHAT_ID = new StringConfigKey(
            "notificator.telegram.chatId",
            List.of(KeyType.CONFIG));

    /**
     * Telegram notification send location message.
     */
    public static final ConfigKey<Boolean> NOTIFICATOR_TELEGRAM_SEND_LOCATION = new BooleanConfigKey(
            "notificator.telegram.sendLocation",
            List.of(KeyType.CONFIG));

    /**
     * Enable user expiration email notification.
     */
    public static final ConfigKey<Boolean> NOTIFICATION_EXPIRATION_USER = new BooleanConfigKey(
            "notification.expiration.user",
            List.of(KeyType.CONFIG));

    /**
     * User expiration reminder. Value in milliseconds.
     */
    public static final ConfigKey<Long> NOTIFICATION_EXPIRATION_USER_REMINDER = new LongConfigKey(
            "notification.expiration.user.reminder",
            List.of(KeyType.CONFIG));

    /**
     * Enable device expiration email notification.
     */
    public static final ConfigKey<Boolean> NOTIFICATION_EXPIRATION_DEVICE = new BooleanConfigKey(
            "notification.expiration.device",
            List.of(KeyType.CONFIG));

    /**
     * Device expiration reminder. Value in milliseconds.
     */
    public static final ConfigKey<Long> NOTIFICATION_EXPIRATION_DEVICE_REMINDER = new LongConfigKey(
            "notification.expiration.device.reminder",
            List.of(KeyType.CONFIG));

    /**
     * Maximum time period for reports in seconds. Can be useful to prevent users to request unreasonably long reports.
     * By default, there is no limit.
     */
    public static final ConfigKey<Long> REPORT_PERIOD_LIMIT = new LongConfigKey(
            "report.periodLimit",
            List.of(KeyType.CONFIG));

    /**
     * Time threshold for fast reports. Fast reports are more efficient, but less accurate and missing some information.
     * The value is in seconds. One day by default.
     */
    public static final ConfigKey<Long> REPORT_FAST_THRESHOLD = new LongConfigKey(
            "report.fastThreshold",
            List.of(KeyType.CONFIG),
            86400L);

    /**
     * Trips less than minimal duration and minimal distance are ignored. 300 seconds and 500 meters are default.
     */
    public static final ConfigKey<Long> REPORT_TRIP_MINIMAL_TRIP_DISTANCE = new LongConfigKey(
            "report.trip.minimalTripDistance",
            List.of(KeyType.CONFIG, KeyType.DEVICE),
            500L);

    /**
     * Trips less than minimal duration and minimal distance are ignored. 300 seconds and 500 meters are default.
     */
    public static final ConfigKey<Long> REPORT_TRIP_MINIMAL_TRIP_DURATION = new LongConfigKey(
            "report.trip.minimalTripDuration",
            List.of(KeyType.CONFIG, KeyType.DEVICE),
            300L);

    /**
     * Parking less than minimal duration does not cut trip. Default 300 seconds.
     */
    public static final ConfigKey<Long> REPORT_TRIP_MINIMAL_PARKING_DURATION = new LongConfigKey(
            "report.trip.minimalParkingDuration",
            List.of(KeyType.CONFIG, KeyType.DEVICE),
            300L);

    /**
     * Gaps of more than specified time are counted as stops. Default value is one hour.
     */
    public static final ConfigKey<Long> REPORT_TRIP_MINIMAL_NO_DATA_DURATION = new LongConfigKey(
            "report.trip.minimalNoDataDuration",
            List.of(KeyType.CONFIG, KeyType.DEVICE),
            3600L);

    /**
     * Flag to enable ignition use for trips calculation.
     */
    public static final ConfigKey<Boolean> REPORT_TRIP_USE_IGNITION = new BooleanConfigKey(
            "report.trip.useIgnition",
            List.of(KeyType.CONFIG, KeyType.DEVICE),
            false);

    /**
     * Ignore odometer value reported by the device and use server-calculated total distance instead. This is useful
     * if device reports invalid or zero odometer values.
     */
    public static final ConfigKey<Boolean> REPORT_IGNORE_ODOMETER = new BooleanConfigKey(
            "report.ignoreOdometer",
            List.of(KeyType.CONFIG),
            false);

    /**
     * Boolean flag to enable or disable position filtering.
     */
    public static final ConfigKey<Boolean> FILTER_ENABLE = new BooleanConfigKey(
            "filter.enable",
            List.of(KeyType.CONFIG));

    /**
     * Filter invalid (valid field is set to false) positions.
     */
    public static final ConfigKey<Boolean> FILTER_INVALID = new BooleanConfigKey(
            "filter.invalid",
            List.of(KeyType.CONFIG));

    /**
     * Filter zero coordinates. Zero latitude and longitude are theoretically valid values, but it practice it usually
     * indicates invalid GPS data.
     */
    public static final ConfigKey<Boolean> FILTER_ZERO = new BooleanConfigKey(
            "filter.zero",
            List.of(KeyType.CONFIG));

    /**
     * Filter duplicate records (duplicates are detected by time value).
     */
    public static final ConfigKey<Boolean> FILTER_DUPLICATE = new BooleanConfigKey(
            "filter.duplicate",
            List.of(KeyType.CONFIG));

    /**
     * Filter messages that do not have GPS location. If they are not filtered, they will include the last known
     * location.
     */
    public static final ConfigKey<Boolean> FILTER_OUTDATED = new BooleanConfigKey(
            "filter.outdated",
            List.of(KeyType.CONFIG));

    /**
     * Filter records with fix time in the future. The value is specified in seconds. Records that have fix time more
     * than the specified number of seconds later than current server time would be filtered out.
     */
    public static final ConfigKey<Long> FILTER_FUTURE = new LongConfigKey(
            "filter.future",
            List.of(KeyType.CONFIG));

    /**
     * Filter records with fix time in the past. The value is specified in seconds. Records that have fix time more
     * than the specified number of seconds before current server time would be filtered out.
     */
    public static final ConfigKey<Long> FILTER_PAST = new LongConfigKey(
            "filter.past",
            List.of(KeyType.CONFIG));

    /**
     * Filter positions with accuracy less than specified value in meters.
     */
    public static final ConfigKey<Integer> FILTER_ACCURACY = new IntegerConfigKey(
            "filter.accuracy",
            List.of(KeyType.CONFIG));

    /**
     * Filter cell and wifi locations that are coming from geolocation provider.
     */
    public static final ConfigKey<Boolean> FILTER_APPROXIMATE = new BooleanConfigKey(
            "filter.approximate",
            List.of(KeyType.CONFIG));

    /**
     * Filter positions with exactly zero speed values.
     */
    public static final ConfigKey<Boolean> FILTER_STATIC = new BooleanConfigKey(
            "filter.static",
            List.of(KeyType.CONFIG));

    /**
     * Filter records by distance. The values is specified in meters. If the new position is less far than this value
     * from the last one it gets filtered out.
     */
    public static final ConfigKey<Integer> FILTER_DISTANCE = new IntegerConfigKey(
            "filter.distance",
            List.of(KeyType.CONFIG));

    /**
     * Filter records by Maximum Speed value in knots. Can be used to filter jumps to far locations even if Position
     * appears valid or if Position `speed` field reported by the device is also within limits. Calculates speed from
     * the distance to the previous position and the elapsed time.
     * Tip: Shouldn't be too low. Start testing with values at about 25000.
     */
    public static final ConfigKey<Integer> FILTER_MAX_SPEED = new IntegerConfigKey(
            "filter.maxSpeed",
            List.of(KeyType.CONFIG));

    /**
     * Filter position if time from previous position is less than specified value in seconds.
     */
    public static final ConfigKey<Integer> FILTER_MIN_PERIOD = new IntegerConfigKey(
            "filter.minPeriod",
            List.of(KeyType.CONFIG));

    /**
     * Filter position if the daily limit is exceeded for the device.
     */
    public static final ConfigKey<Integer> FILTER_DAILY_LIMIT = new IntegerConfigKey(
            "filter.dailyLimit",
            List.of(KeyType.CONFIG));

    /**
     * If false, the server expects all locations to come sequentially (for each device). Filter checks for duplicates,
     * distance, speed, or time period only against the location that was last received by server.
     * If true, the server expects locations to come at random order (since tracking device might go offline).
     * Filter checks for duplicates, distance, speed, or time period against the preceding Position's.
     * Important: setting to true can cause potential performance issues.
     */
    public static final ConfigKey<Boolean> FILTER_RELATIVE = new BooleanConfigKey(
            "filter.relative",
            List.of(KeyType.CONFIG));

    /**
     * Time limit for the filtering in seconds. If the time difference between the last position was received by server
     * and a new position is received by server is more than this limit, the new position will not be filtered out.
     */
    public static final ConfigKey<Long> FILTER_SKIP_LIMIT = new LongConfigKey(
            "filter.skipLimit",
            List.of(KeyType.CONFIG));

    /**
     * Enable attributes skipping. Attribute skipping can be enabled in the config or device attributes.
     * If position contains any attribute mentioned in "filter.skipAttributes" config key, position is not filtered out.
     */
    public static final ConfigKey<Boolean> FILTER_SKIP_ATTRIBUTES_ENABLE = new BooleanConfigKey(
            "filter.skipAttributes.enable",
            List.of(KeyType.CONFIG));

    /**
     * Attribute skipping can be enabled in the config or device attributes.
     * If position contains any attribute mentioned in "filter.skipAttributes" config key, position is not filtered out.
     */
    public static final ConfigKey<String> FILTER_SKIP_ATTRIBUTES = new StringConfigKey(
            "filter.skipAttributes",
            List.of(KeyType.CONFIG, KeyType.DEVICE),
            "");

    /**
     * Override device time. Possible values are 'deviceTime' and 'serverTime'
     */
    public static final ConfigKey<String> TIME_OVERRIDE = new StringConfigKey(
            "time.override",
            List.of(KeyType.CONFIG));

    /**
     * List of protocols to enable. If not specified, Traccar enabled all protocols that have port numbers listed.
     * The value is a comma-separated list of protocol names.
     * Example value: teltonika,osmand
     */
    public static final ConfigKey<String> PROTOCOLS_ENABLE = new StringConfigKey(
            "protocols.enable",
            List.of(KeyType.CONFIG));

    /**
     * List of protocols for overriding time. If not specified override is applied globally. List consist of protocol
     * names that can be separated by comma or single space character.
     */
    public static final ConfigKey<String> TIME_PROTOCOLS = new StringConfigKey(
            "time.protocols",
            List.of(KeyType.CONFIG));

    /**
     * Replaces coordinates with last known if change is less than a 'coordinates.minError' meters
     * or more than a 'coordinates.maxError' meters. Helps to avoid coordinates jumps during parking period
     * or jumps to zero coordinates.
     */
    public static final ConfigKey<Boolean> COORDINATES_FILTER = new BooleanConfigKey(
            "coordinates.filter",
            List.of(KeyType.CONFIG));

    /**
     * Distance in meters. Distances below this value gets handled like explained in 'coordinates.filter'.
     */
    public static final ConfigKey<Integer> COORDINATES_MIN_ERROR = new IntegerConfigKey(
            "coordinates.minError",
            List.of(KeyType.CONFIG));

    /**
     * Distance in meters. Distances above this value gets handled like explained in 'coordinates.filter', but only if
     * Position is also marked as 'invalid'.
     */
    public static final ConfigKey<Integer> COORDINATES_MAX_ERROR = new IntegerConfigKey(
            "coordinates.maxError",
            List.of(KeyType.CONFIG));

    /**
     * Enable to save device IP addresses information. Disabled by default.
     */
    public static final ConfigKey<Boolean> PROCESSING_REMOTE_ADDRESS_ENABLE = new BooleanConfigKey(
            "processing.remoteAddress.enable",
            List.of(KeyType.CONFIG));

    /**
     * Enable copying of missing attributes from last position to the current one. Might be useful if device doesn't
     * send some values in every message.
     */
    public static final ConfigKey<Boolean> PROCESSING_COPY_ATTRIBUTES_ENABLE = new BooleanConfigKey(
            "processing.copyAttributes.enable",
            List.of(KeyType.CONFIG));

    /**
     * List of attributes to copy. Attributes should be separated by a comma without any spacing.
     * For example: alarm,ignition
     */
    public static final ConfigKey<String> PROCESSING_COPY_ATTRIBUTES = new StringConfigKey(
            "processing.copyAttributes",
            List.of(KeyType.CONFIG, KeyType.DEVICE));

    /**
     * Include device attributes in the computed attribute context.
     */
    public static final ConfigKey<Boolean> PROCESSING_COMPUTED_ATTRIBUTES_DEVICE_ATTRIBUTES = new BooleanConfigKey(
            "processing.computedAttributes.deviceAttributes",
            List.of(KeyType.CONFIG));

    /**
     * Include last position attributes in the computed attribute context.
     */
    public static final ConfigKey<Boolean> PROCESSING_COMPUTED_ATTRIBUTES_LAST_ATTRIBUTES = new BooleanConfigKey(
            "processing.computedAttributes.lastAttributes",
            List.of(KeyType.CONFIG));

    /**
     * Enable local variables declaration.
     */
    public static final ConfigKey<Boolean> PROCESSING_COMPUTED_ATTRIBUTES_LOCAL_VARIABLES = new BooleanConfigKey(
            "processing.computedAttributes.localVariables",
            List.of(KeyType.CONFIG));

    /**
     * Enable loops processing.
     */
    public static final ConfigKey<Boolean> PROCESSING_COMPUTED_ATTRIBUTES_LOOPS = new BooleanConfigKey(
            "processing.computedAttributes.loops",
            List.of(KeyType.CONFIG));

    /**
     * Enable new instances creation.
     * When disabled, parsing a script/expression using 'new(...)' will throw a parsing exception;
     */
    public static final ConfigKey<Boolean> PROCESSING_COMPUTED_ATTRIBUTES_NEW_INSTANCE_CREATION = new BooleanConfigKey(
            "processing.computedAttributes.newInstanceCreation",
            List.of(KeyType.CONFIG));

    /**
     * Boolean flag to enable or disable reverse geocoder.
     */
    public static final ConfigKey<Boolean> GEOCODER_ENABLE = new BooleanConfigKey(
            "geocoder.enable",
            List.of(KeyType.CONFIG));

    /**
     * Reverse geocoder type. Check reverse geocoding documentation for more info. By default (if the value is not
     * specified) server uses Google API.
     */
    public static final ConfigKey<String> GEOCODER_TYPE = new StringConfigKey(
            "geocoder.type",
            List.of(KeyType.CONFIG));

    /**
     * Geocoder server URL. Applicable only to Nominatim and Gisgraphy providers.
     */
    public static final ConfigKey<String> GEOCODER_URL = new StringConfigKey(
            "geocoder.url",
            List.of(KeyType.CONFIG));

    /**
     * Provider API key. Most providers require API keys.
     */
    public static final ConfigKey<String> GEOCODER_KEY = new StringConfigKey(
            "geocoder.key",
            List.of(KeyType.CONFIG));

    /**
     * Language parameter for providers that support localization (e.g. Google and Nominatim).
     */
    public static final ConfigKey<String> GEOCODER_LANGUAGE = new StringConfigKey(
            "geocoder.language",
            List.of(KeyType.CONFIG));

    /**
     * Address format string. Default value is %h %r, %t, %s, %c. See AddressFormat for more info.
     */
    public static final ConfigKey<String> GEOCODER_FORMAT = new StringConfigKey(
            "geocoder.format",
            List.of(KeyType.CONFIG));

    /**
     * Cache size for geocoding results.
     */
    public static final ConfigKey<Integer> GEOCODER_CACHE_SIZE = new IntegerConfigKey(
            "geocoder.cacheSize",
            List.of(KeyType.CONFIG));

    /**
     * Disable automatic reverse geocoding requests for all positions.
     */
    public static final ConfigKey<Boolean> GEOCODER_IGNORE_POSITIONS = new BooleanConfigKey(
            "geocoder.ignorePositions",
            List.of(KeyType.CONFIG));

    /**
     * Boolean flag to apply reverse geocoding to invalid positions.
     */
    public static final ConfigKey<Boolean> GEOCODER_PROCESS_INVALID_POSITIONS = new BooleanConfigKey(
            "geocoder.processInvalidPositions",
            List.of(KeyType.CONFIG));

    /**
     * Optional parameter to specify minimum distance for new reverse geocoding request. If distance is less than
     * specified value (in meters), then Traccar will reuse last known address.
     */
    public static final ConfigKey<Integer> GEOCODER_REUSE_DISTANCE = new IntegerConfigKey(
            "geocoder.reuseDistance",
            List.of(KeyType.CONFIG));

    /**
     * Perform geocoding when preparing reports and sending notifications.
     */
    public static final ConfigKey<Boolean> GEOCODER_ON_REQUEST = new BooleanConfigKey(
            "geocoder.onRequest",
            List.of(KeyType.CONFIG));

    /**
     * Boolean flag to enable LBS location resolution. Some devices send cell towers information and WiFi point when GPS
     * location is not available. Traccar can determine coordinates based on that information using third party
     * services. Default value is false.
     */
    public static final ConfigKey<Boolean> GEOLOCATION_ENABLE = new BooleanConfigKey(
            "geolocation.enable",
            List.of(KeyType.CONFIG));

    /**
     * Provider to use for LBS location. Available options: google, unwired and opencellid. By default, google is
     * used. You have to supply a key that you get from corresponding provider. For more information see LBS geolocation
     * documentation.
     */
    public static final ConfigKey<String> GEOLOCATION_TYPE = new StringConfigKey(
            "geolocation.type",
            List.of(KeyType.CONFIG));

    /**
     * Geolocation provider API URL address. Not required for most providers.
     */
    public static final ConfigKey<String> GEOLOCATION_URL = new StringConfigKey(
            "geolocation.url",
            List.of(KeyType.CONFIG));

    /**
     * Provider API key. OpenCellID service requires API key.
     */
    public static final ConfigKey<String> GEOLOCATION_KEY = new StringConfigKey(
            "geolocation.key",
            List.of(KeyType.CONFIG));

    /**
     * Boolean flag to apply geolocation to invalid positions.
     */
    public static final ConfigKey<Boolean> GEOLOCATION_PROCESS_INVALID_POSITIONS = new BooleanConfigKey(
            "geolocation.processInvalidPositions",
            List.of(KeyType.CONFIG));

    /**
     * Reuse last geolocation result if network details have not changed.
     */
    public static final ConfigKey<Boolean> GEOLOCATION_REUSE = new BooleanConfigKey(
            "geolocation.reuse",
            List.of(KeyType.CONFIG));

    /**
     * Process geolocation only when Wi-Fi information is available. This makes the result more accurate.
     */
    public static final ConfigKey<Boolean> GEOLOCATION_REQUIRE_WIFI = new BooleanConfigKey(
            "geolocation.requireWifi",
            List.of(KeyType.CONFIG));

    /**
     * Default MCC value to use if device doesn't report MCC.
     */
    public static final ConfigKey<Integer> GEOLOCATION_MCC = new IntegerConfigKey(
            "geolocation.mcc",
            List.of(KeyType.CONFIG));

    /**
     * Default MNC value to use if device doesn't report MNC.
     */
    public static final ConfigKey<Integer> GEOLOCATION_MNC = new IntegerConfigKey(
            "geolocation.mnc",
            List.of(KeyType.CONFIG));

    /**
     * Boolean flag to enable speed limit API to get speed limit values depending on location. Default value is false.
     */
    public static final ConfigKey<Boolean> SPEED_LIMIT_ENABLE = new BooleanConfigKey(
            "speedLimit.enable",
            List.of(KeyType.CONFIG));

    /**
     * Provider to use for speed limit. Available options: overpass. By default overpass is used.
     */
    public static final ConfigKey<String> SPEED_LIMIT_TYPE = new StringConfigKey(
            "speedLimit.type",
            List.of(KeyType.CONFIG));

    /**
     * Speed limit provider API URL address.
     */
    public static final ConfigKey<String> SPEED_LIMIT_URL = new StringConfigKey(
            "speedLimit.url",
            List.of(KeyType.CONFIG));

    /**
     * Search radius for speed limit. Value is in meters. Default value is 100.
     */
    public static final ConfigKey<Integer> SPEED_LIMIT_ACCURACY = new IntegerConfigKey(
            "speedLimit.accuracy",
            List.of(KeyType.CONFIG),
            100);

    /**
     * Override latitude sign / hemisphere. Useful in cases where value is incorrect because of device bug. Value can be
     * N for North or S for South.
     */
    public static final ConfigKey<String> LOCATION_LATITUDE_HEMISPHERE = new StringConfigKey(
            "location.latitudeHemisphere",
            List.of(KeyType.CONFIG));

    /**
     * Override longitude sign / hemisphere. Useful in cases where value is incorrect because of device bug. Value can
     * be E for East or W for West.
     */
    public static final ConfigKey<String> LOCATION_LONGITUDE_HEMISPHERE = new StringConfigKey(
            "location.longitudeHemisphere",
            List.of(KeyType.CONFIG));

    /**
     * Jetty Request Log Path.
     * The path must include the string "yyyy_mm_dd", which is replaced with the actual date when creating and rolling
     * over the file.
     * Example: ./logs/jetty-yyyy_mm_dd.request.log
     */
    public static final ConfigKey<String> WEB_REQUEST_LOG_PATH = new StringConfigKey(
            "web.requestLog.path",
            List.of(KeyType.CONFIG));

    /**
     * Set the number of days before rotated request log files are deleted.
     */
    public static final ConfigKey<Integer> WEB_REQUEST_LOG_RETAIN_DAYS = new IntegerConfigKey(
            "web.requestLog.retainDays",
            List.of(KeyType.CONFIG));

    /**
     * Disable systemd health checks.
     */
    public static final ConfigKey<Boolean> WEB_DISABLE_HEALTH_CHECK = new BooleanConfigKey(
            "web.disableHealthCheck",
            List.of(KeyType.CONFIG));

    /**
     * Sets SameSite cookie attribute value.
     * Supported options: Lax, Strict, None.
     */
    public static final ConfigKey<String> WEB_SAME_SITE_COOKIE = new StringConfigKey(
            "web.sameSiteCookie",
            List.of(KeyType.CONFIG));

    /**
     * Enables persisting Jetty session to the database
     */
    public static final ConfigKey<Boolean> WEB_PERSIST_SESSION = new BooleanConfigKey(
            "web.persistSession",
            List.of(KeyType.CONFIG));

    /**
     * Public URL for the web app. Used for notification, report link and OpenID Connect.
     * If not provided, Traccar will attempt to get a URL from the server IP address, but it might be a local address.
     */
    public static final ConfigKey<String> WEB_URL = new StringConfigKey(
            "web.url",
            List.of(KeyType.CONFIG));

    /**
     * Show logs from unknown devices.
     */
    public static final ConfigKey<Boolean> WEB_SHOW_UNKNOWN_DEVICES = new BooleanConfigKey(
            "web.showUnknownDevices",
            List.of(KeyType.CONFIG));

    /**
     * Enable commands for a shared device.
     */
    public static final ConfigKey<Boolean> WEB_SHARE_DEVICE_COMMANDS = new BooleanConfigKey(
            "web.shareDevice.commands",
            List.of(KeyType.CONFIG));

    /**
     * Enable reports for a shared device.
     */
    public static final ConfigKey<Boolean> WEB_SHARE_DEVICE_REPORTS = new BooleanConfigKey(
            "web.shareDevice.reports",
            List.of(KeyType.CONFIG));

    /**
     * Output logging to the standard terminal output instead of a log file.
     */
    public static final ConfigKey<Boolean> LOGGER_CONSOLE = new BooleanConfigKey(
            "logger.console",
            List.of(KeyType.CONFIG));

    /**
     * Log executed SQL queries.
     */
    public static final ConfigKey<Boolean> LOGGER_QUERIES = new BooleanConfigKey(
            "logger.queries",
            List.of(KeyType.CONFIG));

    /**
     * Log file name. For rotating logs, a date is added at the end of the file name for non-current logs.
     */
    public static final ConfigKey<String> LOGGER_FILE = new StringConfigKey(
            "logger.file",
            List.of(KeyType.CONFIG));

    /**
     * Logging level. Default value is 'info'.
     * Available options: off, severe, warning, info, config, fine, finer, finest, all.
     */
    public static final ConfigKey<String> LOGGER_LEVEL = new StringConfigKey(
            "logger.level",
            List.of(KeyType.CONFIG));

    /**
     * Print full exception traces. Useful for debugging. By default shortened traces are logged.
     */
    public static final ConfigKey<Boolean> LOGGER_FULL_STACK_TRACES = new BooleanConfigKey(
            "logger.fullStackTraces",
            List.of(KeyType.CONFIG));

    /**
     * Create a new log file daily. Helps with log management. For example, downloading and cleaning logs. Enabled by
     * default.
     */
    public static final ConfigKey<Boolean> LOGGER_ROTATE = new BooleanConfigKey(
            "logger.rotate",
            List.of(KeyType.CONFIG));

    /**
     * Log file rotation interval, the default rotation interval is once a day.
     * This option is ignored if 'logger.rotate' = false
     * Available options: day, hour
     */
    public static final ConfigKey<String> LOGGER_ROTATE_INTERVAL = new StringConfigKey(
            "logger.rotate.interval",
            List.of(KeyType.CONFIG),
            "day");

    /**
     * A list of position attributes to log.
     */
    public static final ConfigKey<String> LOGGER_ATTRIBUTES = new StringConfigKey(
            "logger.attributes",
            List.of(KeyType.CONFIG),
            "time,position,speed,course,accuracy,result");

    /**
     * Broadcast method. Available options are "multicast" and "redis". By default (if the value is not
     * specified or does not matches available options) server disables broadcast.
     */
    public static final ConfigKey<String> BROADCAST_TYPE = new StringConfigKey(
            "broadcast.type",
            List.of(KeyType.CONFIG));

    /**
     * Multicast interface. It can be either an IP address or an interface name.
     */
    public static final ConfigKey<String> BROADCAST_INTERFACE = new StringConfigKey(
            "broadcast.interface",
            List.of(KeyType.CONFIG));

    /**
     * Multicast address or Redis URL for broadcasting synchronization events.
     */
    public static final ConfigKey<String> BROADCAST_ADDRESS = new StringConfigKey(
            "broadcast.address",
            List.of(KeyType.CONFIG));

    /**
     * Multicast port for broadcasting synchronization events.
     */
    public static final ConfigKey<Integer> BROADCAST_PORT = new IntegerConfigKey(
            "broadcast.port",
            List.of(KeyType.CONFIG));

}
