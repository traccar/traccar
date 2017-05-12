/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.database;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.AttributePermission;
import org.traccar.model.Attribute;
import org.traccar.model.Device;
import org.traccar.model.DeviceAttribute;
import org.traccar.model.GroupAttribute;

public class AttributesManager {

    private final DataManager dataManager;

    private final Map<Long, Attribute> attributes = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> deviceAttributes = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> deviceAttributesWithGroups = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> groupAttributes = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> userAttributes = new ConcurrentHashMap<>();

    public AttributesManager(DataManager dataManager) {
        this.dataManager = dataManager;
        refreshAttributes();
    }

    public Set<Long> getUserAttributes(long userId) {
        if (!userAttributes.containsKey(userId)) {
            userAttributes.put(userId, new HashSet<Long>());
        }
        return userAttributes.get(userId);
    }

    public Set<Long> getGroupAttributes(long groupId) {
        if (!groupAttributes.containsKey(groupId)) {
            groupAttributes.put(groupId, new HashSet<Long>());
        }
        return groupAttributes.get(groupId);
    }

    public Set<Long> getDeviceAttributes(long deviceId) {
        return getDeviceAttributes(deviceAttributes, deviceId);
    }

    public Set<Long> getAllDeviceAttributes(long deviceId) {
        return getDeviceAttributes(deviceAttributesWithGroups, deviceId);
    }

    private Set<Long> getDeviceAttributes(Map<Long, Set<Long>> deviceAttributes, long deviceId) {
        if (!deviceAttributes.containsKey(deviceId)) {
            deviceAttributes.put(deviceId, new HashSet<Long>());
        }
        return deviceAttributes.get(deviceId);
    }

    public final void refreshAttributes() {
        if (dataManager != null) {
            try {
                attributes.clear();
                for (Attribute attribute : dataManager.getAttributes()) {
                    attributes.put(attribute.getId(), attribute);
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
        refreshUserAttributes();
        refresh();
    }

    public final void refreshUserAttributes() {
        if (dataManager != null) {
            try {
                userAttributes.clear();
                for (AttributePermission attributePermission : dataManager.getAttributePermissions()) {
                    getUserAttributes(attributePermission.getUserId()).add(attributePermission.getAttributeId());
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }

    public final void refresh() {
        if (dataManager != null) {
            try {

                Collection<GroupAttribute> databaseGroupAttributes = dataManager.getGroupAttributes();

                groupAttributes.clear();
                for (GroupAttribute groupAttribute : databaseGroupAttributes) {
                    getGroupAttributes(groupAttribute.getGroupId()).add(groupAttribute.getAttributeId());
                }

                Collection<DeviceAttribute> databaseDeviceAttributes = dataManager.getDeviceAttributes();
                Collection<Device> allDevices = Context.getDeviceManager().getAllDevices();

                deviceAttributes.clear();
                deviceAttributesWithGroups.clear();

                for (DeviceAttribute deviceAttribute : databaseDeviceAttributes) {
                    getDeviceAttributes(deviceAttribute.getDeviceId())
                        .add(deviceAttribute.getAttributeId());
                    getAllDeviceAttributes(deviceAttribute.getDeviceId())
                        .add(deviceAttribute.getAttributeId());
                }

                for (Device device : allDevices) {
                    long groupId = device.getGroupId();
                    while (groupId != 0) {
                        getAllDeviceAttributes(device.getId()).addAll(getGroupAttributes(groupId));
                        if (Context.getDeviceManager().getGroupById(groupId) != null) {
                            groupId = Context.getDeviceManager().getGroupById(groupId).getGroupId();
                        } else {
                            groupId = 0;
                        }
                    }
                }

            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }

    public void addAttribute(Attribute attribute) throws SQLException {
        dataManager.addAttribute(attribute);
        attributes.put(attribute.getId(), attribute);
    }

    public void updateAttribute(Attribute attribute) throws SQLException {
        dataManager.updateAttribute(attribute);
        Attribute cachedAttribute = attributes.get(attribute.getId());
        cachedAttribute.setDescription(attribute.getDescription());
        cachedAttribute.setAttribute(attribute.getAttribute());
        cachedAttribute.setExpression(attribute.getExpression());
        cachedAttribute.setType(attribute.getType());
    }

    public void removeAttribute(long computedAttributeId) throws SQLException {
        dataManager.removeAttribute(computedAttributeId);
        attributes.remove(computedAttributeId);
        refreshUserAttributes();
        refresh();
    }

    public boolean checkAttribute(long userId, long attributeId) {
        return getUserAttributes(userId).contains(attributeId);
    }

    public Attribute getAttribute(long id) {
        return attributes.get(id);
    }

    public final Collection<Attribute> getAttributes(Set<Long> attributeIds) {
        Collection<Attribute> result = new LinkedList<>();
        for (long attributeId : attributeIds) {
            result.add(getAttribute(attributeId));
        }
        return result;
    }

    public final Set<Long> getAllAttributes() {
        return attributes.keySet();
    }

    public final Set<Long> getManagedAttributes(long userId) {
        Set<Long> attributes = new HashSet<>();
        attributes.addAll(getUserAttributes(userId));
        for (long managedUserId : Context.getPermissionsManager().getUserPermissions(userId)) {
            attributes.addAll(getUserAttributes(managedUserId));
        }
        return attributes;
    }

}
