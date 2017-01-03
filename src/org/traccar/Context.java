/*
 * Copyright 2015 - 2016 Anton Tananaev (anton@traccar.org)
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
package org.traccar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ning.http.client.AsyncHttpClient;

import java.net.InetAddress;
import java.util.Properties;

import org.apache.velocity.app.VelocityEngine;
import org.eclipse.jetty.util.URIUtil;
import org.traccar.database.AliasesManager;
import org.traccar.database.CalendarManager;
import org.traccar.database.ConnectionManager;
import org.traccar.database.DataManager;
import org.traccar.database.DeviceManager;
import org.traccar.database.IdentityManager;
import org.traccar.database.NotificationManager;
import org.traccar.database.PermissionsManager;
import org.traccar.database.GeofenceManager;
import org.traccar.database.StatisticsManager;
import org.traccar.geocoder.BingMapsGeocoder;
import org.traccar.geocoder.FactualGeocoder;
import org.traccar.geocoder.GeocodeFarmGeocoder;
import org.traccar.geocoder.GisgraphyGeocoder;
import org.traccar.geocoder.GoogleGeocoder;
import org.traccar.geocoder.MapQuestGeocoder;
import org.traccar.geocoder.NominatimGeocoder;
import org.traccar.geocoder.OpenCageGeocoder;
import org.traccar.geocoder.Geocoder;
import org.traccar.helper.Log;
import org.traccar.geolocation.GoogleGeolocationProvider;
import org.traccar.geolocation.GeolocationProvider;
import org.traccar.geolocation.MozillaGeolocationProvider;
import org.traccar.geolocation.OpenCellIdGeolocationProvider;
import org.traccar.notification.EventForwarder;
import org.traccar.web.WebServer;

public final class Context {

    private Context() {
    }

    private static Config config;

    public static Config getConfig() {
        return config;
    }

    private static boolean loggerEnabled;

    public static boolean isLoggerEnabled() {
        return loggerEnabled;
    }

    private static ObjectMapper objectMapper;

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    private static IdentityManager identityManager;

    public static IdentityManager getIdentityManager() {
        return identityManager;
    }

    private static DataManager dataManager;

    public static DataManager getDataManager() {
        return dataManager;
    }

    private static DeviceManager deviceManager;

    public static DeviceManager getDeviceManager() {
        return deviceManager;
    }

    private static ConnectionManager connectionManager;

    public static ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    private static PermissionsManager permissionsManager;

    public static PermissionsManager getPermissionsManager() {
        return permissionsManager;
    }

    private static Geocoder geocoder;

    public static Geocoder getGeocoder() {
        return geocoder;
    }

    private static GeolocationProvider geolocationProvider;

    public static GeolocationProvider getGeolocationProvider() {
        return geolocationProvider;
    }

    private static WebServer webServer;

    public static WebServer getWebServer() {
        return webServer;
    }

    private static ServerManager serverManager;

    public static ServerManager getServerManager() {
        return serverManager;
    }

    private static GeofenceManager geofenceManager;

    public static GeofenceManager getGeofenceManager() {
        return geofenceManager;
    }

    private static CalendarManager calendarManager;

    public static CalendarManager getCalendarManager() {
        return calendarManager;
    }

    private static NotificationManager notificationManager;

    public static NotificationManager getNotificationManager() {
        return notificationManager;
    }

    private static VelocityEngine velocityEngine;

    public static VelocityEngine getVelocityEngine() {
        return velocityEngine;
    }

    private static final AsyncHttpClient ASYNC_HTTP_CLIENT = new AsyncHttpClient();

    public static AsyncHttpClient getAsyncHttpClient() {
        return ASYNC_HTTP_CLIENT;
    }

    private static EventForwarder eventForwarder;

    public static EventForwarder getEventForwarder() {
        return eventForwarder;
    }

    private static AliasesManager aliasesManager;

    public static AliasesManager getAliasesManager() {
        return aliasesManager;
    }

    private static StatisticsManager statisticsManager;

    public static StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }

    public static void init(String[] arguments) throws Exception {

        config = new Config();
        if (arguments.length <= 0) {
            throw new RuntimeException("Configuration file is not provided");
        }

        config.load(arguments[0]);

        loggerEnabled = config.getBoolean("logger.enable");
        if (loggerEnabled) {
            Log.setupLogger(config);
        }

        objectMapper = new ObjectMapper();
        objectMapper.setConfig(
                objectMapper.getSerializationConfig().without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

        if (config.hasKey("database.url")) {
            dataManager = new DataManager(config);
        }

        if (dataManager != null) {
            deviceManager = new DeviceManager(dataManager);
        }

        identityManager = deviceManager;

        if (config.getBoolean("geocoder.enable")) {
            String type = config.getString("geocoder.type", "google");
            String url = config.getString("geocoder.url");
            String key = config.getString("geocoder.key");

            int cacheSize = config.getInteger("geocoder.cacheSize");
            switch (type) {
                case "nominatim":
                    if (key != null) {
                        geocoder = new NominatimGeocoder(url, key, cacheSize);
                    } else {
                        geocoder = new NominatimGeocoder(url, cacheSize);
                    }
                    break;
                case "gisgraphy":
                    geocoder = new GisgraphyGeocoder(url, cacheSize);
                    break;
                case "mapquest":
                    geocoder = new MapQuestGeocoder(url, key, cacheSize);
                    break;
                case "opencage":
                    geocoder = new OpenCageGeocoder(url, key, cacheSize);
                    break;
                case "bingmaps":
                    geocoder = new BingMapsGeocoder(url, key, cacheSize);
                    break;
                case "factual":
                    geocoder = new FactualGeocoder(url, key, cacheSize);
                    break;
                case "geocodefarm":
                    if (key != null) {
                        geocoder = new GeocodeFarmGeocoder(key, cacheSize);
                    } else {
                        geocoder = new GeocodeFarmGeocoder(cacheSize);
                    }
                default:
                    if (key != null) {
                        geocoder = new GoogleGeocoder(key, cacheSize);
                    } else {
                        geocoder = new GoogleGeocoder(cacheSize);
                    }
                    break;
            }
        }

        if (config.getBoolean("geolocation.enable")) {
            String type = config.getString("geolocation.type", "mozilla");
            String key = config.getString("geolocation.key");

            switch (type) {
                case "google":
                    geolocationProvider = new GoogleGeolocationProvider(key);
                    break;
                case "opencellid":
                    geolocationProvider = new OpenCellIdGeolocationProvider(key);
                    break;
                default:
                    if (key != null) {
                        geolocationProvider = new MozillaGeolocationProvider(key);
                    } else {
                        geolocationProvider = new MozillaGeolocationProvider();
                    }
                    break;
            }
        }

        if (config.getBoolean("web.enable")) {
            webServer = new WebServer(config, dataManager.getDataSource());
        }

        permissionsManager = new PermissionsManager(dataManager);

        connectionManager = new ConnectionManager();

        if (config.getBoolean("event.geofenceHandler")) {
            geofenceManager = new GeofenceManager(dataManager);
            calendarManager = new CalendarManager(dataManager);
        }

        if (config.getBoolean("event.enable")) {
            notificationManager = new NotificationManager(dataManager);
            Properties velocityProperties = new Properties();
            velocityProperties.setProperty("file.resource.loader.path",
                    Context.getConfig().getString("mail.templatesPath", "templates/mail") + "/");
            velocityProperties.setProperty("runtime.log.logsystem.class",
                    "org.apache.velocity.runtime.log.NullLogChute");

            String address = config.getString("web.address", InetAddress.getLocalHost().getHostAddress());
            int port = config.getInteger("web.port", 8082);
            String webUrl = URIUtil.newURI("http", address, port, "", "");
            webUrl = Context.getConfig().getString("web.url", webUrl);
            velocityProperties.setProperty("web.url", webUrl);

            velocityEngine = new VelocityEngine();
            velocityEngine.init(velocityProperties);
        }

        serverManager = new ServerManager();

        if (config.getBoolean("event.forward.enable")) {
            eventForwarder = new EventForwarder();
        }

        aliasesManager = new AliasesManager(dataManager);

        statisticsManager = new StatisticsManager();

    }

    public static void init(IdentityManager testIdentityManager) {
        config = new Config();
        objectMapper = new ObjectMapper();
        identityManager = testIdentityManager;
    }

}
