/*
 * Copyright 2022 - 2023 Anton Tananaev (anton@traccar.org)
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
package org.traccar.helper.model;

import org.traccar.api.security.PermissionsService;
import org.traccar.config.Config;
import org.traccar.config.ConfigKey;
import org.traccar.config.KeyType;
import org.traccar.config.Keys;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Server;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

public final class AttributeUtil {

    private AttributeUtil() {
    }

    public interface Provider {
        Device getDevice();
        Group getGroup(long groupId);
        Server getServer();
        Config getConfig();
    }

    public static <T> T lookup(CacheManager cacheManager, ConfigKey<T> key, long deviceId) {
        return lookup(new CacheProvider(cacheManager, deviceId), key);
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    public static <T> T lookup(Provider provider, ConfigKey<T> key) {
        Device device = provider.getDevice();
        Object result = device.getAttributes().get(key.getKey());
        long groupId = device.getGroupId();
        while (result == null && groupId > 0) {
            Group group = provider.getGroup(groupId);
            if (group != null) {
                result = group.getAttributes().get(key.getKey());
                groupId = group.getGroupId();
            } else {
                groupId = 0;
            }
        }
        if (result == null && key.hasType(KeyType.SERVER)) {
            result = provider.getServer().getAttributes().get(key.getKey());
        }
        if (result == null && key.hasType(KeyType.CONFIG)) {
            result = provider.getConfig().getString(key.getKey());
        }

        if (result != null) {
            Class<T> valueClass = key.getValueClass();
            if (valueClass.equals(Boolean.class)) {
                return (T) (result instanceof String
                        ? Boolean.parseBoolean((String) result)
                        : result);
            } else if (valueClass.equals(Integer.class)) {
                return (T) (Object) (result instanceof String
                        ? Integer.parseInt((String) result)
                        : ((Number) result).intValue());
            } else if (valueClass.equals(Long.class)) {
                return (T) (Object) (result instanceof String
                        ? Long.parseLong((String) result)
                        : ((Number) result).longValue());
            } else if (valueClass.equals(Double.class)) {
                return (T) (Object) (result instanceof String
                        ? Double.parseDouble((String) result)
                        : ((Number) result).doubleValue());
            } else {
                return (T) result;
            }
        }
        return key.getDefaultValue();
    }

    public static String getDevicePassword(
            CacheManager cacheManager, long deviceId, String protocol, String defaultPassword) {

        String password = lookup(cacheManager, Keys.DEVICE_PASSWORD, deviceId);
        if (password != null) {
            return password;
        }

        if (protocol != null) {
            password = cacheManager.getConfig().getString(Keys.PROTOCOL_DEVICE_PASSWORD.withPrefix(protocol));
            if (password != null) {
                return password;
            }
        }

        return defaultPassword;
    }

    public static class CacheProvider implements Provider {

        private final CacheManager cacheManager;
        private final long deviceId;

        public CacheProvider(CacheManager cacheManager, long deviceId) {
            this.cacheManager = cacheManager;
            this.deviceId = deviceId;
        }

        @Override
        public Device getDevice() {
            return cacheManager.getObject(Device.class, deviceId);
        }

        @Override
        public Group getGroup(long groupId) {
            return cacheManager.getObject(Group.class, groupId);
        }

        @Override
        public Server getServer() {
            return cacheManager.getServer();
        }

        @Override
        public Config getConfig() {
            return cacheManager.getConfig();
        }
    }

    public static class StorageProvider implements Provider {

        private final Config config;
        private final Storage storage;
        private final PermissionsService permissionsService;
        private final Device device;

        public StorageProvider(Config config, Storage storage, PermissionsService permissionsService, Device device) {
            this.config = config;
            this.storage = storage;
            this.permissionsService = permissionsService;
            this.device = device;
        }

        @Override
        public Device getDevice() {
            return device;
        }

        @Override
        public Group getGroup(long groupId) {
            try {
                return storage.getObject(
                        Group.class, new Request(new Columns.All(), new Condition.Equals("id", groupId)));
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Server getServer() {
            try {
                return permissionsService.getServer();
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Config getConfig() {
            return config;
        }
    }

}
