/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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

import org.traccar.config.ConfigKey;
import org.traccar.config.KeyType;
import org.traccar.config.Keys;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.session.cache.CacheManager;

public final class AttributeUtil {

    private AttributeUtil() {
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    public static <T> T lookup(CacheManager cacheManager, ConfigKey<T> key, long deviceId) {
        Device device = cacheManager.getObject(Device.class, deviceId);
        Object result = device.getAttributes().get(key.getKey());
        long groupId = device.getGroupId();
        while (result == null && groupId > 0) {
            Group group = cacheManager.getObject(Group.class, groupId);
            if (group != null) {
                result = group.getAttributes().get(key.getKey());
                groupId = group.getGroupId();
            } else {
                groupId = 0;
            }
        }
        if (result == null && key.hasType(KeyType.SERVER)) {
            result = cacheManager.getServer().getAttributes().get(key.getKey());
        }
        if (result == null && key.hasType(KeyType.CONFIG)) {
            result = cacheManager.getConfig().getString(key.getKey());
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

}
