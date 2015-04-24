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

import org.traccar.database.DataCache;
import org.traccar.database.DataManager;
import org.traccar.geocode.GoogleReverseGeocoder;
import org.traccar.geocode.NominatimReverseGeocoder;
import org.traccar.geocode.ReverseGeocoder;
import org.traccar.helper.Log;
import org.traccar.http.WebServer;

import java.io.FileInputStream;
import java.util.Properties;

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

    private static DataCache dataCache;

    public static DataCache getDataCache() {
        return dataCache;
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
        dataCache = new DataCache(dataManager);

        if (Boolean.parseBoolean(properties.getProperty("geocoder.enable"))) {
            String type = properties.getProperty("geocoder.type");
            if (type != null && type.equals("nominatim")) {
                reverseGeocoder = new NominatimReverseGeocoder(properties.getProperty("geocoder.url"));
            } else {
                reverseGeocoder = new GoogleReverseGeocoder();
            }
        }

        if (Boolean.valueOf(properties.getProperty("http.enable"))) {
            webServer = new WebServer();
        }

        serverManager = new ServerManager();
        serverManager.init();
    }

    public static void init(DataManager dataManager) {
        Context.dataManager = dataManager;
    }

}
