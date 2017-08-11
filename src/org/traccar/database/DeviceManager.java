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
import org.traccar.model.DeviceState;
import org.traccar.model.DeviceTotalDistance;
import org.traccar.model.Group;
import org.traccar.model.Position;
import org.traccar.model.Server;

public class DeviceManager extends BaseObjectManager<Device> implements IdentityManager, ManagableObjects {

    public static final long DEFAULT_REFRESH_DELAY = 300;

    private final Config config;
    private final long dataRefreshDelay;
    private boolean lookupGroupsAttribute;

    private Map<String, Device> devicesByUniqueId;
    private Map<String, Device> devicesByPhone;
    private AtomicLong devicesLastUpdate = new AtomicLong();

    private final Map<Long, Position> positions = new ConcurrentHashMap<>();

    private final Map<Long, DeviceState> deviceStates = new ConcurrentHashMap<>();

    private boolean fallbackToText;

    public DeviceManager(DataManager dataManager) {
        super(dataManager, Device.class);
        this.config = Context.getConfig();
        if (devicesByPhone == null) {
            devicesByPhone = new ConcurrentHashMap<>();
        }
        if (devicesByUniqueId == null) {
            devicesByUniqueId = new ConcurrentHashMap<>();
        }
        dataRefreshDelay = config.getLong("database.refreshDelay", DEFAULT_REFRESH_DELAY) * 1000;
        lookupGroupsAttribute = config.getBoolean("deviceManager.lookupGroupsAttribute");
        fallbackToText = config.getBoolean("command.fallbackToSms");
        refreshLastPositions();
    }

    private void updateDeviceCache(boolean force) throws SQLException {
        long lastUpdate = devicesLastUpdate.get();
        if ((force || System.currentTimeMillis() - lastUpdate > dataRefreshDelay)
                && devicesLastUpdate.compareAndSet(lastUpdate, System.currentTimeMillis())) {
            refreshItems();
        }
    }

    @Override
    public Device getByUniqueId(String uniqueId) throws SQLException {
        boolean forceUpdate = !devicesByUniqueId.containsKey(uniqueId) && !config.getBoolean("database.ignoreUnknown");

        updateDeviceCache(forceUpdate);

        return devicesByUniqueId.get(uniqueId);
    }

    public Device getDeviceByPhone(String phone) {
        return devicesByPhone.get(phone);
    }

    @Override
    public Set<Long> getAllItems() {
        Set<Long> result = super.getAllItems();
        if (result.isEmpty()) {
            try {
                updateDeviceCache(true);
            } catch (SQLException e) {
                Log.warning(e);
            }
            result = super.getAllItems();
        }
        return result;
    }

    public Collection<Device> getAllDevices() {
        return getItems(getAllItems());
    }

    @Override
    public Set<Long> getUserItems(long userId) {
        if (Context.getPermissionsManager() != null) {
            return Context.getPermissionsManager().getDevicePermissions(userId);
        } else {
            return new HashSet<>();
        }
    }

    @Override
    public Set<Long> getManagedItems(long userId) {
        Set<Long> result = new HashSet<>();
        result.addAll(getUserItems(userId));
        for (long managedUserId : Context.getUsersManager().getUserItems(userId)) {
            result.addAll(getUserItems(managedUserId));
        }
        return result;
    }

    private void putUniqueDeviceId(Device device) {
        if (devicesByUniqueId == null) {
            devicesByUniqueId = new ConcurrentHashMap<>(getAllItems().size());
        }
        devicesByUniqueId.put(device.getUniqueId(), device);
    }

    private void putPhone(Device device) {
        if (devicesByPhone == null) {
            devicesByPhone = new ConcurrentHashMap<>(getAllItems().size());
        }
        devicesByPhone.put(device.getPhone(), device);
    }

    @Override
    protected void addNewItem(Device device) {
        super.addNewItem(device);
        putUniqueDeviceId(device);
        if (device.getPhone() != null  && !device.getPhone().isEmpty()) {
            putPhone(device);
        }
        if (Context.getGeofenceManager() != null) {
            Position lastPosition = getLastPosition(device.getId());
            if (lastPosition != null) {
                device.setGeofenceIds(Context.getGeofenceManager().getCurrentDeviceGeofences(lastPosition));
            }
        }
    }

    @Override
    protected void updateCachedItem(Device device) {
        Device cachedDevice = getById(device.getId());
        cachedDevice.setName(device.getName());
        cachedDevice.setGroupId(device.getGroupId());
        cachedDevice.setCategory(device.getCategory());
        cachedDevice.setContact(device.getContact());
        cachedDevice.setModel(device.getModel());
        cachedDevice.setAttributes(device.getAttributes());
        if (!device.getUniqueId().equals(cachedDevice.getUniqueId())) {
            devicesByUniqueId.remove(cachedDevice.getUniqueId());
            cachedDevice.setUniqueId(device.getUniqueId());
            putUniqueDeviceId(cachedDevice);
        }
        if (device.getPhone() != null && !device.getPhone().isEmpty()
                && !device.getPhone().equals(cachedDevice.getPhone())) {
            devicesByPhone.remove(cachedDevice.getPhone());
            cachedDevice.setPhone(device.getPhone());
            putPhone(cachedDevice);
        }
    }

    @Override
    protected void removeCachedItem(long deviceId) {
        Device cachedDevice = getById(deviceId);
        if (cachedDevice != null) {
            String deviceUniqueId = cachedDevice.getUniqueId();
            String phone = cachedDevice.getPhone();
            super.removeCachedItem(deviceId);
            devicesByUniqueId.remove(deviceUniqueId);
            if (phone != null && !phone.isEmpty()) {
                devicesByPhone.remove(phone);
            }
        }
        positions.remove(deviceId);
    }

    public void updateDeviceStatus(Device device) throws SQLException {
        getDataManager().updateDeviceStatus(device);
        Device cachedDevice = getById(device.getId());
        if (cachedDevice != null) {
            cachedDevice.setStatus(device.getStatus());
        }
    }

    private void refreshLastPositions() {
        if (getDataManager() != null) {
            try {
                for (Position position : getDataManager().getLatestPositions()) {
                    positions.put(position.getDeviceId(), position);
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }

    public boolean isLatestPosition(Position position) {
        Position lastPosition = getLastPosition(position.getDeviceId());
        return lastPosition == null || position.getFixTime().compareTo(lastPosition.getFixTime()) >= 0;
    }

    public void updateLatestPosition(Position position) throws SQLException {

        if (isLatestPosition(position)) {

            getDataManager().updateLatestPosition(position);

            Device device = getById(position.getDeviceId());
            if (device != null) {
                device.setPositionId(position.getId());
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
            for (long deviceId : getUserItems(userId)) {
                if (positions.containsKey(deviceId)) {
                    result.add(positions.get(deviceId));
                }
            }
        }

        return result;
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
        Device device = getById(deviceId);
        if (device != null) {
            result = device.getString(attributeName);
            if (result == null && lookupGroupsAttribute) {
                long groupId = device.getGroupId();
                while (groupId != 0) {
                    Group group = Context.getGroupsManager().getById(groupId);
                    if (group != null) {
                        result = group.getString(attributeName);
                        if (result != null) {
                            break;
                        }
                        groupId = group.getGroupId();
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
            getDataManager().addPosition(last);
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
                protocol.sendTextCommand(getById(deviceId).getPhone(), command);
            } else if (command.getType().equals(Command.TYPE_CUSTOM)) {
                Context.getSmppManager().sendMessageSync(getById(deviceId).getPhone(),
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

    public DeviceState getDeviceState(long deviceId) {
        DeviceState deviceState = deviceStates.get(deviceId);
        if (deviceState == null) {
            deviceState = new DeviceState();
            deviceStates.put(deviceId, deviceState);
        }
        return deviceState;
    }

    public void setDeviceState(long deviceId, DeviceState deviceState) {
        deviceStates.put(deviceId, deviceState);
    }

}
