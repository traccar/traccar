/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.io.FileInputStream;
import java.util.Properties;
import org.traccar.database.ConnectionManager;
import org.traccar.database.DataManager;
import org.traccar.database.PermissionsManager;
import org.traccar.geocode.GisgraphyReverseGeocoder;
import org.traccar.geocode.GoogleReverseGeocoder;
import org.traccar.geocode.NominatimReverseGeocoder;
import org.traccar.geocode.ReverseGeocoder;
import org.traccar.helper.Log;
import org.traccar.http.WebServer;

public class Context {

    private static Properties properties;

    public static Properties getProps() {
        return properties;
    }

    private static boolean loggerEnabled;

    public static boolean isLoggerEnabled() {
        return loggerEnabled;
    }

    private static DataManager dataManager;

    public static DataManager getDataManager() {
        return dataManager;
    }

    private static ConnectionManager connectionManager;

    public static ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    private static PermissionsManager permissionsManager;

    public static PermissionsManager getPermissionsManager() {
        return permissionsManager;
    }

    private static ReverseGeocoder reverseGeocoder;

    public static ReverseGeocoder getReverseGeocoder() {
        return reverseGeocoder;
    }

    private static WebServer webServer;

    public static WebServer getWebServer() {
        return webServer;
    }

    private static ServerManager serverManager;

    public static ServerManager getServerManager() {
        return serverManager;
    }

    public static void init(String[] arguments) throws Exception {

        properties = new Properties();
        if (arguments.length > 0) {
            properties.loadFromXML(new FileInputStream(arguments[0]));
        }

        loggerEnabled = Boolean.valueOf(properties.getProperty("logger.enable"));
        if (loggerEnabled) {
            Log.setupLogger(properties);
        }

        dataManager = new DataManager(properties);
        connectionManager = new ConnectionManager();
        if (!Boolean.valueOf(properties.getProperty("web.old"))) {
            permissionsManager = new PermissionsManager();
        }

        if (Boolean.parseBoolean(properties.getProperty("geocoder.enable"))) {
            String type = properties.getProperty("geocoder.type");
            if (type != null && type.equals("nominatim")) {
                reverseGeocoder = new NominatimReverseGeocoder(properties.getProperty("geocoder.url"));
            } else if (type != null && type.equals("gisgraphy")) {
                reverseGeocoder = new GisgraphyReverseGeocoder(properties.getProperty("geocoder.url"));
            } else {
                reverseGeocoder = new GoogleReverseGeocoder();
            }
        }

        if (Boolean.valueOf(properties.getProperty("web.enable"))) {
            webServer = new WebServer();
        }

        serverManager = new ServerManager();

        dataManager.initDatabaseSchema();
        connectionManager.init(dataManager);
        serverManager.init();
    }

    /**
     * Initialize context for unit testing
     */
    public static void init(DataManager dataManager) {
        properties = new Properties();
        Context.dataManager = dataManager;
    }

}
