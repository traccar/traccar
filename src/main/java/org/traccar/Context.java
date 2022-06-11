/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
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
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.BaseObjectManager;
import org.traccar.database.DataManager;
import org.traccar.database.DeviceManager;
import org.traccar.database.GroupsManager;
import org.traccar.database.IdentityManager;
import org.traccar.database.NotificationManager;
import org.traccar.database.PermissionsManager;
import org.traccar.database.UsersManager;
import org.traccar.geocoder.Geocoder;
import org.traccar.helper.Log;
import org.traccar.model.BaseModel;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Notification;
import org.traccar.model.User;
import org.traccar.notification.EventForwarder;
import org.traccar.notification.NotificatorManager;
import org.traccar.session.ConnectionManager;
import org.traccar.session.cache.CacheManager;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.ext.ContextResolver;

public final class Context {

    private Context() {
    }

    private static Config config;

    public static Config getConfig() {
        return config;
    }

    private static IdentityManager identityManager;

    public static IdentityManager getIdentityManager() {
        return identityManager;
    }

    private static DataManager dataManager;

    public static DataManager getDataManager() {
        return dataManager;
    }

    private static UsersManager usersManager;

    public static UsersManager getUsersManager() {
        return usersManager;
    }

    private static GroupsManager groupsManager;

    public static GroupsManager getGroupsManager() {
        return groupsManager;
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

    private static NotificationManager notificationManager;

    public static NotificationManager getNotificationManager() {
        return notificationManager;
    }

    private static Client client = ClientBuilder.newClient();

    public static Client getClient() {
        return client;
    }

    private static class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

        @Override
        public ObjectMapper getContext(Class<?> clazz) {
            return Main.getInjector().getInstance(ObjectMapper.class);
        }

    }

    public static void init(String configFile) throws Exception {

        try {
            config = new Config(configFile);
            Log.setupLogger(config);
        } catch (Exception e) {
            config = new Config();
            Log.setupDefaultLogger();
            throw e;
        }

        client = ClientBuilder.newClient().register(new ObjectMapperContextResolver());

        if (config.hasKey(Keys.DATABASE_URL)) {
            dataManager = new DataManager(config);
        }

        if (dataManager != null) {
            usersManager = new UsersManager(dataManager);
            groupsManager = new GroupsManager(dataManager);
            deviceManager = new DeviceManager(dataManager);
        }

        identityManager = deviceManager;

        permissionsManager = new PermissionsManager(dataManager, usersManager);

        connectionManager = new ConnectionManager();

        initEventsModule();

    }

    private static void initEventsModule() {

        notificationManager = new NotificationManager(
                dataManager,
                Main.getInjector().getInstance(CacheManager.class),
                Main.getInjector().getInstance(EventForwarder.class),
                Main.getInjector().getInstance(NotificatorManager.class),
                Main.getInjector().getInstance(Geocoder.class));
    }

    public static <T extends BaseModel> BaseObjectManager<T> getManager(Class<T> clazz) {
        if (clazz.equals(Device.class)) {
            return (BaseObjectManager<T>) deviceManager;
        } else if (clazz.equals(Group.class)) {
            return (BaseObjectManager<T>) groupsManager;
        } else if (clazz.equals(User.class)) {
            return (BaseObjectManager<T>) usersManager;
        } else if (clazz.equals(Notification.class)) {
            return (BaseObjectManager<T>) notificationManager;
        }
        return null;
    }

}
