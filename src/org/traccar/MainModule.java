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
import org.traccar.database.IdentityManager;
import org.traccar.handler.DistanceHandler;
import org.traccar.handler.FilterHandler;
import org.traccar.handler.RemoteAddressHandler;

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
    public static IdentityManager provideIdentityManager() {
        return Context.getIdentityManager();
    }

    @Provides
    public static Client provideClient() {
        return Context.getClient();
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

    @Override
    protected void configure() {
        binder().requireExplicitBindings();
    }

}
