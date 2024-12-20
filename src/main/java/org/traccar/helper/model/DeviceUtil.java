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

import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.User;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;

public final class DeviceUtil {

    private DeviceUtil() {
    }

    public static void resetStatus(Storage storage) throws StorageException {
        storage.updateObject(new Device(), new Request(new Columns.Include("status")));
    }


    public static Collection<Device> getAccessibleDevices(
            Storage storage, long userId,
            Collection<Long> deviceIds, Collection<Long> groupIds) throws StorageException {

        var devices = storage.getObjects(Device.class, new Request(
                new Columns.All(),
                new Condition.Permission(User.class, userId, Device.class)));
        var deviceById = devices.stream()
                .collect(Collectors.toUnmodifiableMap(Device::getId, x -> x));
        var devicesByGroup = devices.stream()
                .filter(x -> x.getGroupId() > 0)
                .collect(Collectors.groupingBy(Device::getGroupId));

        var groups = storage.getObjects(Group.class, new Request(
                new Columns.All(),
                new Condition.Permission(User.class, userId, Group.class)));
        var groupsByGroup = groups.stream()
                .filter(x -> x.getGroupId() > 0)
                .collect(Collectors.groupingBy(Group::getGroupId));

        var results = deviceIds.stream()
                .map(deviceById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        var groupQueue = new LinkedList<>(groupIds);
        while (!groupQueue.isEmpty()) {
            long groupId = groupQueue.pop();
            results.addAll(devicesByGroup.getOrDefault(groupId, Collections.emptyList()));
            groupQueue.addAll(groupsByGroup.getOrDefault(groupId, Collections.emptyList())
                    .stream().map(Group::getId).toList());
        }

        return results;
    }

}
