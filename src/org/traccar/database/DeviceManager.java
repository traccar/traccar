/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.traccar.BaseProtocol;
import org.traccar.Config;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Command;
import org.traccar.model.CommandType;
import org.traccar.model.Device;
import org.traccar.model.DeviceTotalDistance;
import org.traccar.model.Group;
import org.traccar.model.Position;
import org.traccar.model.Server;

public class DeviceManager implements IdentityManager {

    public static final long DEFAULT_REFRESH_DELAY = 300;

    private final Config config;
    private final DataManager dataManager;
    private final long dataRefreshDelay;
    private boolean lookupGroupsAttribute;

    private Map<Long, Device> devicesById;
    private Map<String, Device> devicesByUniqueId;
    private Map<String, Device> devicesByPhone;
    private AtomicLong devicesLastUpdate = new AtomicLong();

    private Map<Long, Group> groupsById;
    private AtomicLong groupsLastUpdate = new AtomicLong();

    private final Map<Long, Position> positions = new ConcurrentHashMap<>();

    private boolean fallbackToText;

    public DeviceManager(DataManager dataManager) {
        this.dataManager = dataManager;
        this.config = Context.getConfig();
        dataRefreshDelay = config.getLong("database.refreshDelay", DEFAULT_REFRESH_DELAY) * 1000;
        lookupGroupsAttribute = config.getBoolean("deviceManager.lookupGroupsAttribute");
        fallbackToText = config.getBoolean("command.fallbackToSms");
        if (dataManager != null) {
            try {
                updateGroupCache(true);
                updateDeviceCache(true);
                for (Position position : dataManager.getLatestPositions()) {
                    positions.put(position.getDeviceId(), position);
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }

    private void updateDeviceCache(boolean force) throws SQLException {

        long lastUpdate = devicesLastUpdate.get();
        if ((force || System.currentTimeMillis() - lastUpdate > dataRefreshDelay)
                && devicesLastUpdate.compareAndSet(lastUpdate, System.currentTimeMillis())) {
            GeofenceManager geofenceManager = Context.getGeofenceManager();
            Collection<Device> databaseDevices = dataManager.getAllDevices();
            if (devicesById == null) {
                devicesById = new ConcurrentHashMap<>(databaseDevices.size());
            }
            if (devicesByUniqueId == null) {
                devicesByUniqueId = new ConcurrentHashMap<>(databaseDevices.size());
            }
            if (devicesByPhone == null) {
                devicesByPhone = new ConcurrentHashMap<>(databaseDevices.size());
            }
            Set<Long> databaseDevicesIds = new HashSet<>();
            Set<String> databaseDevicesUniqueIds = new HashSet<>();
            Set<String> databaseDevicesPhones = new HashSet<>();
            for (Device device : databaseDevices) {
                databaseDevicesIds.add(device.getId());
                databaseDevicesUniqueIds.add(device.getUniqueId());
                databaseDevicesPhones.add(device.getPhone());
                if (devicesById.containsKey(device.getId())) {
                    Device cachedDevice = devicesById.get(device.getId());
                    cachedDevice.setName(device.getName());
                    cachedDevice.setGroupId(device.getGroupId());
                    cachedDevice.setCategory(device.getCategory());
                    cachedDevice.setContact(device.getContact());
                    cachedDevice.setModel(device.getModel());
                    cachedDevice.setAttributes(device.getAttributes());
                    if (!device.getUniqueId().equals(cachedDevice.getUniqueId())) {
                        devicesByUniqueId.put(device.getUniqueId(), cachedDevice);
                    }
                    cachedDevice.setUniqueId(device.getUniqueId());
                    if (device.getPhone() != null && !device.getPhone().isEmpty()
                            && !device.getPhone().equals(cachedDevice.getPhone())) {
                        devicesByPhone.put(device.getPhone(), cachedDevice);
                    }
                    cachedDevice.setPhone(device.getPhone());
                } else {
                    devicesById.put(device.getId(), device);
                    devicesByUniqueId.put(device.getUniqueId(), device);
                    if (device.getPhone() != null && !device.getPhone().isEmpty()) {
                        devicesByPhone.put(device.getPhone(), device);
                    }
                    if (geofenceManager != null) {
                        Position lastPosition = getLastPosition(device.getId());
                        if (lastPosition != null) {
                            device.setGeofenceIds(geofenceManager.getCurrentDeviceGeofences(lastPosition));
                        }
                    }
                }
            }
            for (Iterator<Long> iterator = devicesById.keySet().iterator(); iterator.hasNext();) {
                if (!databaseDevicesIds.contains(iterator.next())) {
                    iterator.remove();
                }
            }
            for (Iterator<String> iterator = devicesByUniqueId.keySet().iterator(); iterator.hasNext();) {
                if (!databaseDevicesUniqueIds.contains(iterator.next())) {
                    iterator.remove();
                }
            }
            for (Iterator<String> iterator = devicesByPhone.keySet().iterator(); iterator.hasNext();) {
                if (!databaseDevicesPhones.contains(iterator.next())) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public Device getDeviceById(long id) {
        return devicesById.get(id);
    }

    @Override
    public Device getDeviceByUniqueId(String uniqueId) throws SQLException {
        boolean forceUpdate = !devicesByUniqueId.containsKey(uniqueId) && !config.getBoolean("database.ignoreUnknown");

        updateDeviceCache(forceUpdate);

        return devicesByUniqueId.get(uniqueId);
    }

    public Device getDeviceByPhone(String phone) {
        return devicesByPhone.get(phone);
    }

    public Collection<Device> getAllDevices() {
        boolean forceUpdate = devicesById.isEmpty();

        try {
            updateDeviceCache(forceUpdate);
        } catch (SQLException e) {
            Log.warning(e);
        }

        return devicesById.values();
    }

    public Collection<Device> getDevices(long userId) throws SQLException {
        Collection<Device> devices = new ArrayList<>();
        for (long id : Context.getPermissionsManager().getDevicePermissions(userId)) {
            devices.add(devicesById.get(id));
        }
        return devices;
    }

    public Collection<Device> getManagedDevices(long userId) throws SQLException {
        Collection<Device> devices = new HashSet<>();
        devices.addAll(getDevices(userId));
        for (long managedUserId : Context.getPermissionsManager().getUserPermissions(userId)) {
            devices.addAll(getDevices(managedUserId));
        }
        return devices;
    }

    public void addDevice(Device device) throws SQLException {
        dataManager.addDevice(device);

        devicesById.put(device.getId(), device);
        devicesByUniqueId.put(device.getUniqueId(), device);
        if (device.getPhone() != null  && !device.getPhone().isEmpty()) {
            devicesByPhone.put(device.getPhone(), device);
        }
    }

    public void updateDevice(Device device) throws SQLException {
        dataManager.updateDevice(device);

        devicesById.put(device.getId(), device);
        devicesByUniqueId.put(device.getUniqueId(), device);
        if (device.getPhone() != null && !device.getPhone().isEmpty()) {
            devicesByPhone.put(device.getPhone(), device);
        }
    }

    public void updateDeviceStatus(Device device) throws SQLException {
        dataManager.updateDeviceStatus(device);
        if (devicesById.containsKey(device.getId())) {
            Device cachedDevice = devicesById.get(device.getId());
            cachedDevice.setStatus(device.getStatus());
        }
    }

    public void removeDevice(long deviceId) throws SQLException {
        dataManager.removeDevice(deviceId);

        if (devicesById.containsKey(deviceId)) {
            String deviceUniqueId = devicesById.get(deviceId).getUniqueId();
            String phone = devicesById.get(deviceId).getPhone();
            devicesById.remove(deviceId);
            devicesByUniqueId.remove(deviceUniqueId);
            if (phone != null && !phone.isEmpty()) {
                devicesByPhone.remove(phone);
            }
        }
        positions.remove(deviceId);
    }

    public boolean isLatestPosition(Position position) {
        Position lastPosition = getLastPosition(position.getDeviceId());
        return lastPosition == null || position.getFixTime().compareTo(lastPosition.getFixTime()) >= 0;
    }

    public void updateLatestPosition(Position position) throws SQLException {

        if (isLatestPosition(position)) {

            dataManager.updateLatestPosition(position);

            if (devicesById.containsKey(position.getDeviceId())) {
                devicesById.get(position.getDeviceId()).setPositionId(position.getId());
            }

            positions.put(position.getDeviceId(), position);

            if (Context.getConnectionManager() != null) {
                Context.getConnectionManager().updatePosition(position);
            }
        }
    }

    @Override
    public Position getLastPosition(long deviceId) {
        return positions.get(deviceId);
    }

    public Collection<Position> getInitialState(long userId) {

        List<Position> result = new LinkedList<>();

        if (Context.getPermissionsManager() != null) {
            for (long deviceId : Context.getPermissionsManager().getDevicePermissions(userId)) {
                if (positions.containsKey(deviceId)) {
                    result.add(positions.get(deviceId));
                }
            }
        }

        return result;
    }

    private void updateGroupCache(boolean force) throws SQLException {

        long lastUpdate = groupsLastUpdate.get();
        if ((force || System.currentTimeMillis() - lastUpdate > dataRefreshDelay)
                && groupsLastUpdate.compareAndSet(lastUpdate, System.currentTimeMillis())) {
            Collection<Group> databaseGroups = dataManager.getAllGroups();
            if (groupsById == null) {
                groupsById = new ConcurrentHashMap<>(databaseGroups.size());
            }
            Set<Long> databaseGroupsIds = new HashSet<>();
            for (Group group : databaseGroups) {
                databaseGroupsIds.add(group.getId());
                if (groupsById.containsKey(group.getId())) {
                    Group cachedGroup = groupsById.get(group.getId());
                    cachedGroup.setName(group.getName());
                    cachedGroup.setGroupId(group.getGroupId());
                } else {
                    groupsById.put(group.getId(), group);
                }
            }
            for (Long cachedGroupId : groupsById.keySet()) {
                if (!databaseGroupsIds.contains(cachedGroupId)) {
                    devicesById.remove(cachedGroupId);
                }
            }
            databaseGroupsIds.clear();
        }
    }

    public Group getGroupById(long id) {
        return groupsById.get(id);
    }

    public Collection<Group> getAllGroups() {
        boolean forceUpdate = groupsById.isEmpty();

        try {
            updateGroupCache(forceUpdate);
        } catch (SQLException e) {
            Log.warning(e);
        }

        return groupsById.values();
    }

    public Collection<Group> getGroups(long userId) throws SQLException {
        Collection<Group> groups = new ArrayList<>();
        for (long id : Context.getPermissionsManager().getGroupPermissions(userId)) {
            groups.add(getGroupById(id));
        }
        return groups;
    }

    public Collection<Group> getManagedGroups(long userId) throws SQLException {
        Collection<Group> groups = new ArrayList<>();
        groups.addAll(getGroups(userId));
        for (long managedUserId : Context.getPermissionsManager().getUserPermissions(userId)) {
            groups.addAll(getGroups(managedUserId));
        }
        return groups;
    }

    private void checkGroupCycles(Group group) {
        Set<Long> groups = new HashSet<>();
        while (group != null) {
            if (groups.contains(group.getId())) {
                throw new IllegalArgumentException("Cycle in group hierarchy");
            }
            groups.add(group.getId());
            group = groupsById.get(group.getGroupId());
        }
    }

    public void addGroup(Group group) throws SQLException {
        checkGroupCycles(group);
        dataManager.addGroup(group);
        groupsById.put(group.getId(), group);
    }

    public void updateGroup(Group group) throws SQLException {
        checkGroupCycles(group);
        dataManager.updateGroup(group);
        groupsById.put(group.getId(), group);
    }

    public void removeGroup(long groupId) throws SQLException {
        dataManager.removeGroup(groupId);
        groupsById.remove(groupId);
    }

    public boolean lookupAttributeBoolean(
            long deviceId, String attributeName, boolean defaultValue, boolean lookupConfig) {
        String result = lookupAttribute(deviceId, attributeName, lookupConfig);
        if (result != null) {
            return Boolean.parseBoolean(result);
        }
        return defaultValue;
    }

    public String lookupAttributeString(
            long deviceId, String attributeName, String defaultValue, boolean lookupConfig) {
        String result = lookupAttribute(deviceId, attributeName, lookupConfig);
        if (result != null) {
            return result;
        }
        return defaultValue;
    }

    public int lookupAttributeInteger(long deviceId, String attributeName, int defaultValue, boolean lookupConfig) {
        String result = lookupAttribute(deviceId, attributeName, lookupConfig);
        if (result != null) {
            return Integer.parseInt(result);
        }
        return defaultValue;
    }

    public long lookupAttributeLong(
            long deviceId, String attributeName, long defaultValue, boolean lookupConfig) {
        String result = lookupAttribute(deviceId, attributeName, lookupConfig);
        if (result != null) {
            return Long.parseLong(result);
        }
        return defaultValue;
    }

    public double lookupAttributeDouble(
            long deviceId, String attributeName, double defaultValue, boolean lookupConfig) {
        String result = lookupAttribute(deviceId, attributeName, lookupConfig);
        if (result != null) {
            return Double.parseDouble(result);
        }
        return defaultValue;
    }

    private String lookupAttribute(long deviceId, String attributeName, boolean lookupConfig) {
        String result = null;
        Device device = getDeviceById(deviceId);
        if (device != null) {
            result = device.getString(attributeName);
            if (result == null && lookupGroupsAttribute) {
                long groupId = device.getGroupId();
                while (groupId != 0) {
                    if (getGroupById(groupId) != null) {
                        result = getGroupById(groupId).getString(attributeName);
                        if (result != null) {
                            break;
                        }
                        groupId = getGroupById(groupId).getGroupId();
                    } else {
                        groupId = 0;
                    }
                }
            }
            if (result == null) {
                if (lookupConfig) {
                    result = Context.getConfig().getString(attributeName);
                } else {
                    Server server = Context.getPermissionsManager().getServer();
                    result = server.getString(attributeName);
                }
            }
        }
        return result;
    }

    public void resetTotalDistance(DeviceTotalDistance deviceTotalDistance) throws SQLException {
        Position last = positions.get(deviceTotalDistance.getDeviceId());
        if (last != null) {
            last.getAttributes().put(Position.KEY_TOTAL_DISTANCE, deviceTotalDistance.getTotalDistance());
            dataManager.addPosition(last);
            updateLatestPosition(last);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void sendCommand(Command command) throws Exception {
        long deviceId = command.getDeviceId();
        if (command.getTextChannel()) {
            Position lastPosition = getLastPosition(deviceId);
            if (lastPosition != null) {
                BaseProtocol protocol = Context.getServerManager().getProtocol(lastPosition.getProtocol());
                protocol.sendTextCommand(devicesById.get(deviceId).getPhone(), command);
            } else if (command.getType().equals(Command.TYPE_CUSTOM)) {
                Context.getSmppManager().sendMessageSync(devicesById.get(deviceId).getPhone(),
                        command.getString(Command.KEY_DATA), true);
            } else {
                throw new RuntimeException("Command " + command.getType() + " is not supported");
            }
        } else {
            ActiveDevice activeDevice = Context.getConnectionManager().getActiveDevice(deviceId);
            if (activeDevice != null) {
                activeDevice.sendCommand(command);
            } else {
                if (fallbackToText) {
                    command.setTextChannel(true);
                    sendCommand(command);
                } else {
                    throw new RuntimeException("Device is not online");
                }
            }
        }
    }

    public Collection<CommandType> getCommandTypes(long deviceId, boolean textChannel) {
        List<CommandType> result = new ArrayList<>();
        Position lastPosition = Context.getDeviceManager().getLastPosition(deviceId);
        if (lastPosition != null) {
            BaseProtocol protocol = Context.getServerManager().getProtocol(lastPosition.getProtocol());
            Collection<String> commands;
            commands = textChannel ? protocol.getSupportedTextCommands() : protocol.getSupportedDataCommands();
            for (String commandKey : commands) {
                result.add(new CommandType(commandKey));
            }
        } else {
            result.add(new CommandType(Command.TYPE_CUSTOM));
        }
        return result;
    }
}
