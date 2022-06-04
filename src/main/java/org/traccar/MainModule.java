/*
 * Copyright 2018 - 2022 Anton Tananaev (anton@traccar.org)
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
import com.google.inject.Scopes;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.CalendarManager;
import org.traccar.database.LdapProvider;
import org.traccar.session.ConnectionManager;
import org.traccar.database.DataManager;
import org.traccar.database.DeviceManager;
import org.traccar.database.GeofenceManager;
import org.traccar.database.IdentityManager;
import org.traccar.database.StatisticsManager;
import org.traccar.geocoder.AddressFormat;
import org.traccar.geocoder.BanGeocoder;
import org.traccar.geocoder.BingMapsGeocoder;
import org.traccar.geocoder.FactualGeocoder;
import org.traccar.geocoder.GeoapifyGeocoder;
import org.traccar.geocoder.GeocodeFarmGeocoder;
import org.traccar.geocoder.GeocodeXyzGeocoder;
import org.traccar.geocoder.Geocoder;
import org.traccar.geocoder.GisgraphyGeocoder;
import org.traccar.geocoder.GoogleGeocoder;
import org.traccar.geocoder.HereGeocoder;
import org.traccar.geocoder.MapQuestGeocoder;
import org.traccar.geocoder.MapTilerGeocoder;
import org.traccar.geocoder.MapboxGeocoder;
import org.traccar.geocoder.MapmyIndiaGeocoder;
import org.traccar.geocoder.NominatimGeocoder;
import org.traccar.geocoder.OpenCageGeocoder;
import org.traccar.geocoder.PositionStackGeocoder;
import org.traccar.geocoder.TomTomGeocoder;
import org.traccar.geolocation.GeolocationProvider;
import org.traccar.geolocation.GoogleGeolocationProvider;
import org.traccar.geolocation.MozillaGeolocationProvider;
import org.traccar.geolocation.OpenCellIdGeolocationProvider;
import org.traccar.geolocation.UnwiredGeolocationProvider;
import org.traccar.handler.GeocoderHandler;
import org.traccar.handler.GeolocationHandler;
import org.traccar.handler.SpeedLimitHandler;
import org.traccar.reports.common.TripsConfig;
import org.traccar.sms.SmsManager;
import org.traccar.speedlimit.OverpassSpeedLimitProvider;
import org.traccar.speedlimit.SpeedLimitProvider;
import org.traccar.storage.Storage;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;

public class MainModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Timer.class).to(HashedWheelTimer.class).in(Scopes.SINGLETON);
    }

    @Provides
    public static ObjectMapper provideObjectMapper() {
        return Context.getObjectMapper();
    }

    @Provides
    public static Config provideConfig() {
        return Context.getConfig();
    }

    @Provides
    public static Storage provideStorage() {
        return Context.getDataManager().getStorage();
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
    public static ConnectionManager provideConnectionManager() {
        return Context.getConnectionManager();
    }

    @Provides
    public static Client provideClient() {
        return Context.getClient();
    }

    @Provides
    public static TripsConfig provideTripsConfig() {
        return Context.getTripsConfig();
    }

    @Provides
    public static DeviceManager provideDeviceManager() {
        return Context.getDeviceManager();
    }

    @Provides
    public static GeofenceManager provideGeofenceManager() {
        return Context.getGeofenceManager();
    }

    @Provides
    public static CalendarManager provideCalendarManager() {
        return Context.getCalendarManager();
    }

    @Provides
    public static SmsManager provideSmsManager() {
        return Context.getSmsManager();
    }

    @Singleton
    @Provides
    public static LdapProvider provideLdapProvider(Config config) {
        if (config.hasKey(Keys.LDAP_URL)) {
            return new LdapProvider(config);
        }
        return null;
    }

    @Singleton
    @Provides
    public static Geocoder provideGeocoder(Config config) {
        if (config.getBoolean(Keys.GEOCODER_ENABLE)) {
            String type = config.getString(Keys.GEOCODER_TYPE, "google");
            String url = config.getString(Keys.GEOCODER_URL);
            String id = config.getString(Keys.GEOCODER_ID);
            String key = config.getString(Keys.GEOCODER_KEY);
            String language = config.getString(Keys.GEOCODER_LANGUAGE);
            String formatString = config.getString(Keys.GEOCODER_FORMAT);
            AddressFormat addressFormat = formatString != null ? new AddressFormat(formatString) : new AddressFormat();

            int cacheSize = config.getInteger(Keys.GEOCODER_CACHE_SIZE);
            switch (type) {
                case "nominatim":
                    return new NominatimGeocoder(url, key, language, cacheSize, addressFormat);
                case "gisgraphy":
                    return new GisgraphyGeocoder(url, cacheSize, addressFormat);
                case "mapquest":
                    return new MapQuestGeocoder(url, key, cacheSize, addressFormat);
                case "opencage":
                    return new OpenCageGeocoder(url, key, language, cacheSize, addressFormat);
                case "bingmaps":
                    return new BingMapsGeocoder(url, key, cacheSize, addressFormat);
                case "factual":
                    return new FactualGeocoder(url, key, cacheSize, addressFormat);
                case "geocodefarm":
                    return new GeocodeFarmGeocoder(key, language, cacheSize, addressFormat);
                case "geocodexyz":
                    return new GeocodeXyzGeocoder(key, cacheSize, addressFormat);
                case "ban":
                    return new BanGeocoder(cacheSize, addressFormat);
                case "here":
                    return new HereGeocoder(url, id, key, language, cacheSize, addressFormat);
                case "mapmyindia":
                    return new MapmyIndiaGeocoder(url, key, cacheSize, addressFormat);
                case "tomtom":
                    return new TomTomGeocoder(url, key, cacheSize, addressFormat);
                case "positionstack":
                    return new PositionStackGeocoder(key, cacheSize, addressFormat);
                case "mapbox":
                    return new MapboxGeocoder(key, cacheSize, addressFormat);
                case "maptiler":
                    return new MapTilerGeocoder(key, cacheSize, addressFormat);
                case "geoapify":
                    return new GeoapifyGeocoder(key, language, cacheSize, addressFormat);
                default:
                    return new GoogleGeocoder(key, language, cacheSize, addressFormat);
            }
        }
        return null;
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
                    return new OpenCellIdGeolocationProvider(url, key);
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
    public static SpeedLimitProvider provideSpeedLimitProvider(Config config) {
        if (config.getBoolean(Keys.SPEED_LIMIT_ENABLE)) {
            String type = config.getString(Keys.SPEED_LIMIT_TYPE, "overpass");
            String url = config.getString(Keys.SPEED_LIMIT_URL);
            switch (type) {
                case "overpass":
                default:
                    return new OverpassSpeedLimitProvider(url);
            }
        }
        return null;
    }

    @Provides
    public static GeolocationHandler provideGeolocationHandler(
            Config config, @Nullable GeolocationProvider geolocationProvider, StatisticsManager statisticsManager) {
        if (geolocationProvider != null) {
            return new GeolocationHandler(config, geolocationProvider, statisticsManager);
        }
        return null;
    }

    @Provides
    public static GeocoderHandler provideGeocoderHandler(
            Config config, @Nullable Geocoder geocoder, IdentityManager identityManager) {
        if (geocoder != null) {
            return new GeocoderHandler(config, geocoder, identityManager);
        }
        return null;
    }

    @Provides
    public static SpeedLimitHandler provideSpeedLimitHandler(@Nullable SpeedLimitProvider speedLimitProvider) {
        if (speedLimitProvider != null) {
            return new SpeedLimitHandler(speedLimitProvider);
        }
        return null;
    }

}
