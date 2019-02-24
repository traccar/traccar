/*
 * Copyright 2018 - 2019 Anton Tananaev (anton@traccar.org)
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
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.DataManager;
import org.traccar.database.IdentityManager;
import org.traccar.database.StatisticsManager;
import org.traccar.geolocation.GeolocationProvider;
import org.traccar.geolocation.GoogleGeolocationProvider;
import org.traccar.geolocation.MozillaGeolocationProvider;
import org.traccar.geolocation.OpenCellIdGeolocationProvider;
import org.traccar.geolocation.UnwiredGeolocationProvider;
import org.traccar.handler.DistanceHandler;
import org.traccar.handler.FilterHandler;
import org.traccar.handler.GeolocationHandler;
import org.traccar.handler.HemisphereHandler;
import org.traccar.handler.RemoteAddressHandler;

import javax.annotation.Nullable;
import javax.ws.rs.client.Client;

public class MainModule extends AbstractModule {

    @Provides
    public static ObjectMapper provideObjectMapper() {
        return Context.getObjectMapper();
    }

    @Provides
    public static Config provideConfig() {
        return Context.getConfig();
    }

    @Provides
    public static DataManager provideDataManager() {
        return Context.getDataManager();
    }

    @Provides
    public static IdentityManager provideIdentityManager() {
        return Context.getIdentityManager();
    }

    @Provides
    public static Client provideClient() {
        return Context.getClient();
    }

    @Singleton
    @Provides
    public static StatisticsManager provideStatisticsManager(Config config, DataManager dataManager, Client client) {
        return new StatisticsManager(config, dataManager, client);
    }

    @Singleton
    @Provides
    public static GeolocationProvider provideGeolocationProvider(Config config) {
        if (config.getBoolean(Keys.GEOLOCATION_ENABLE)) {
            String type = config.getString(Keys.GEOLOCATION_TYPE, "mozilla");
            String url = config.getString(Keys.GEOLOCATION_URL);
            String key = config.getString(Keys.GEOLOCATION_KEY);
            switch (type) {
                case "google":
                    return new GoogleGeolocationProvider(key);
                case "opencellid":
                    return new OpenCellIdGeolocationProvider(key);
                case "unwired":
                    return new UnwiredGeolocationProvider(url, key);
                default:
                    return new MozillaGeolocationProvider(key);
            }
        }
        return null;
    }

    @Singleton
    @Provides
    public static DistanceHandler provideDistanceHandler(Config config, IdentityManager identityManager) {
        return new DistanceHandler(config, identityManager);
    }

    @Singleton
    @Provides
    public static FilterHandler provideFilterHandler(Config config) {
        if (config.getBoolean(Keys.FILTER_ENABLE)) {
            return new FilterHandler(config);
        }
        return null;
    }

    @Singleton
    @Provides
    public static HemisphereHandler provideHemisphereHandler(Config config) {
        if (config.hasKey(Keys.LOCATION_LATITUDE_HEMISPHERE) || config.hasKey(Keys.LOCATION_LONGITUDE_HEMISPHERE)) {
            return new HemisphereHandler(config);
        }
        return null;
    }

    @Singleton
    @Provides
    public static RemoteAddressHandler provideRemoteAddressHandler(Config config) {
        if (config.getBoolean(Keys.PROCESSING_REMOTE_ADDRESS_ENABLE)) {
            return new RemoteAddressHandler();
        }
        return null;
    }

    @Singleton
    @Provides
    public static WebDataHandler provideWebDataHandler(
            Config config, IdentityManager identityManager, ObjectMapper objectMapper, Client client) {
        if (config.getBoolean(Keys.FORWARD_ENABLE)) {
            return new WebDataHandler(config, identityManager, objectMapper, client);
        }
        return null;
    }

    @Singleton
    @Provides
    public static GeolocationHandler provideGeolocationHandler(
            Config config, @Nullable GeolocationProvider geolocationProvider, StatisticsManager statisticsManager) {
        if (geolocationProvider != null) {
            return new GeolocationHandler(config, geolocationProvider, statisticsManager);
        }
        return null;
    }

    @Override
    protected void configure() {
        binder().requireExplicitBindings();
    }

}
