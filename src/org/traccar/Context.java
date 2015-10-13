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

import com.ning.http.client.AsyncHttpClient;
import org.traccar.database.ConnectionManager;
import org.traccar.database.DataManager;
import org.traccar.database.IdentityManager;
import org.traccar.database.PermissionsManager;
import org.traccar.geocode.BingMapsReverseGeocoder;
import org.traccar.geocode.FactualReverseGeocoder;
import org.traccar.geocode.GisgraphyReverseGeocoder;
import org.traccar.geocode.GoogleReverseGeocoder;
import org.traccar.geocode.MapQuestReverseGeocoder;
import org.traccar.geocode.NominatimReverseGeocoder;
import org.traccar.geocode.OpenCageReverseGeocoder;
import org.traccar.geocode.ReverseGeocoder;
import org.traccar.helper.Log;
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

    private static IdentityManager identityManager;

    public static IdentityManager getIdentityManager() {
        return identityManager;
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

    private static final AsyncHttpClient ASYNC_HTTP_CLIENT = new AsyncHttpClient();

    public static AsyncHttpClient getAsyncHttpClient() {
        return ASYNC_HTTP_CLIENT;
    }

    public static void init(String[] arguments) throws Exception {

        config = new Config();
        if (arguments.length > 0) {
            config.load(arguments[0]);
        }

        loggerEnabled = config.getBoolean("logger.enable");
        if (loggerEnabled) {
            Log.setupLogger(config);
        }

        if (config.hasKey("database.url")) {
            dataManager = new DataManager(config);
        }
        identityManager = dataManager;

        if (config.getBoolean("geocoder.enable")) {
            String type = config.getString("geocoder.type", "google");
            String url = config.getString("geocoder.url");
            String key = config.getString("geocoder.key");

            int cacheSize = config.getInteger("geocoder.cacheSize");
            switch (type) {
                case "nominatim":
                    reverseGeocoder = new NominatimReverseGeocoder(url, cacheSize);
                    break;
                case "gisgraphy":
                    reverseGeocoder = new GisgraphyReverseGeocoder(url, cacheSize);
                    break;
                case "mapquest":
                    reverseGeocoder = new MapQuestReverseGeocoder(url, key, cacheSize);
                    break;
                case "opencage":
                    reverseGeocoder = new OpenCageReverseGeocoder(url, key, cacheSize);
                    break;
                case "bingmaps":
                    reverseGeocoder = new BingMapsReverseGeocoder(url, key, cacheSize);
                    break;
                case "factual":
                    reverseGeocoder = new FactualReverseGeocoder(url, key, cacheSize);
                    break;
                default:
                    reverseGeocoder = new GoogleReverseGeocoder(cacheSize);
                    break;
            }
        }

        if (config.getBoolean("web.enable")) {
            if (config.getString("web.type", "new").equals("new")
                    || config.getString("web.type", "new").equals("api")) {
                permissionsManager = new PermissionsManager(dataManager);
            }
            webServer = new WebServer(config, dataManager.getDataSource());
        }

        connectionManager = new ConnectionManager(dataManager);

        serverManager = new ServerManager();
    }

    public static void init(IdentityManager testIdentityManager) {
        config = new Config();
        connectionManager = new ConnectionManager(null);
        identityManager = testIdentityManager;
    }

}
