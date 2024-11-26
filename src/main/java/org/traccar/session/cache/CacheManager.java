/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
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
package org.traccar.session.cache;

import org.traccar.config.Config;
import org.traccar.model.BaseModel;
import org.traccar.model.ObjectOperation;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.model.Notification;

import java.util.Set;

public interface CacheManager {
    <T extends BaseModel> T getObject(Class<T> clazz, long id);

    <T extends BaseModel> Set<T> getDeviceObjects(long deviceId, Class<T> clazz);

    Position getPosition(long deviceId);

    void updatePosition(Position position);

    void addDevice(long deviceId, Object key) throws Exception;

    void removeDevice(long deviceId, Object key);

    <T extends BaseModel> void invalidateObject(boolean local, Class<T> clazz, long id, ObjectOperation operation)
            throws Exception;

    <T1 extends BaseModel, T2 extends BaseModel> void invalidatePermission(
            boolean local, Class<T1> clazz1, long id1, Class<T2> clazz2, long id2, boolean link) throws Exception;

    Server getServer();

    Set<User> getNotificationUsers(long notificationId, long deviceId);

    Set<Notification> getDeviceNotifications(long deviceId);

    Config getConfig();
}
