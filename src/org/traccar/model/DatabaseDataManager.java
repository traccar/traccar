/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.traccar.helper.AdvancedConnection;
import org.traccar.helper.Log;
import org.traccar.helper.NamedParameterStatement;

/**
 * Database abstraction class
 */
public class DatabaseDataManager implements DataManager {

    public DatabaseDataManager(Properties properties)
            throws ClassNotFoundException, SQLException {
        initDatabase(properties);
    }

    /**
     * Database statements
     */
    private NamedParameterStatement queryGetDevices;
    private NamedParameterStatement queryAddDevice;
    private NamedParameterStatement queryUpdateDevice;
    private NamedParameterStatement queryRemoveDevice;
    private NamedParameterStatement queryGetPositions;
    private NamedParameterStatement queryAddPosition;

    /**
     * Initialize database
     */
    private void initDatabase(Properties properties)
            throws ClassNotFoundException, SQLException {

        // Load driver
        String driver = properties.getProperty("database.driver");
        if (driver != null) {
            Class.forName(driver);
        }

        // Refresh delay
        String refreshDelay = properties.getProperty("database.refreshDelay");
        if (refreshDelay != null) {
            devicesRefreshDelay = Long.valueOf(refreshDelay) * 1000;
        } else {
            devicesRefreshDelay = new Long(300) * 1000; // Magic number
        }

        // Connect database
        String url = properties.getProperty("database.url");
        String user = properties.getProperty("database.user");
        String password = properties.getProperty("database.password");
        AdvancedConnection connection = new AdvancedConnection(url, user, password);

        // Load statements from configuration
        String query;

        query = properties.getProperty("database.selectDevice");
        if (query != null) {
            queryGetDevices = new NamedParameterStatement(connection, query);
        }

        query = properties.getProperty("database.insertDevice");
        if (query != null) {
            queryAddDevice = new NamedParameterStatement(connection, query);
        }

        query = properties.getProperty("database.updateDevice");
        if (query != null) {
            queryUpdateDevice = new NamedParameterStatement(connection, query);
        }

        query = properties.getProperty("database.deleteDevice");
        if (query != null) {
            queryRemoveDevice = new NamedParameterStatement(connection, query);
        }

        query = properties.getProperty("database.selectPosition");
        if (query != null) {
            queryGetPositions = new NamedParameterStatement(connection, query);
        }

        query = properties.getProperty("database.insertPosition");
        if (query != null) {
            queryAddPosition = new NamedParameterStatement(connection, query);
        }

        // Create database schema
        query = properties.getProperty("database.initialize");
        if (query != null) try {
            NamedParameterStatement initializeQuery = new NamedParameterStatement(connection, query);
            initializeQuery.prepare();
            initializeQuery.executeUpdate();
        } catch (Exception error) {
            Log.warning(error.getMessage());
        }
    }

    public synchronized List<Device> getDevices() throws SQLException {

        List<Device> deviceList = new LinkedList<Device>();

        queryGetDevices.prepare();
        ResultSet result = queryGetDevices.executeQuery();
        while (result.next()) {
            Device device = new Device();
            device.setId(result.getLong("id"));
            device.setImei(result.getString("imei"));
            deviceList.add(device);
        }

        return deviceList;
    }

    public synchronized void addDevice(Device device) throws SQLException {

        queryAddDevice.prepare();
        queryAddDevice.setString("imei", device.getImei());
        queryAddDevice.executeUpdate();

        // Find generated id
        ResultSet result = queryAddDevice.getGeneratedKeys();
        if (result.next()) {
            device.setId(result.getLong(1));
        }

        devices = null;
    }

    public synchronized void updateDevice(Device device) throws SQLException {

        queryUpdateDevice.prepare();
        queryUpdateDevice.setLong("id", device.getId());
        queryUpdateDevice.setString("imei", device.getImei());
        queryUpdateDevice.executeUpdate();

        devices = null;
    }

    public synchronized void removeDevice(Device device) throws SQLException {

        queryRemoveDevice.prepare();
        queryRemoveDevice.setLong("id", device.getId());
        queryRemoveDevice.executeUpdate();

        devices = null;
    }

    /**
     * Devices cache
     */
    private Map<String, Device> devices;
    private Calendar devicesLastUpdate;
    private Long devicesRefreshDelay;

    public Device getDeviceByImei(String imei) throws SQLException {

        if ((devices == null) || (Calendar.getInstance().getTimeInMillis() - devicesLastUpdate.getTimeInMillis() > devicesRefreshDelay)) {
            devices = new HashMap<String, Device>();
            for (Device device: getDevices()) {
                devices.put(device.getImei(), device);
            }
            devicesLastUpdate = Calendar.getInstance();
        }

        return devices.get(imei);
    }

    public Device getDeviceByPhoneNumber(String phoneNumber) {
        // TODO: implement getDeviceByPhoneNumber
        return null;
    }

    public Device getDeviceByUniqueId(String uniqueId) {
        // TODO: implement getDeviceByUniqueId
        return null;
    }

    public synchronized List<Position> getPositions(Long deviceId) throws SQLException {

        List<Position> positionList = new LinkedList<Position>();

        queryGetPositions.prepare();
        queryGetPositions.setLong("device_id", deviceId);
        ResultSet result = queryGetPositions.executeQuery();
        while (result.next()) {
            // TODO: include other parameters
            Position position = new Position();
            position.setDeviceId(result.getLong("device_id"));
            position.setTime(result.getTimestamp("time"));
            position.setValid(result.getBoolean("valid"));
            position.setLatitude(result.getDouble("latitude"));
            position.setLongitude(result.getDouble("longitude"));
            position.setSpeed(result.getDouble("speed"));
            position.setCourse(result.getDouble("course"));
            position.setPower(result.getDouble("power"));
            position.setMode(result.getInt("mode"));
            position.setAddress(result.getString("address"));
            positionList.add(position);
        }

        return positionList;
    }

    public synchronized void addPosition(Position position) throws SQLException {

        queryAddPosition.prepare();

        queryAddPosition.setLong("device_id", position.getDeviceId());
        queryAddPosition.setTimestamp("time", position.getTime());
        queryAddPosition.setBoolean("valid", position.getValid());
        queryAddPosition.setDouble("altitude", position.getAltitude());
        queryAddPosition.setDouble("latitude", position.getLatitude());
        queryAddPosition.setDouble("longitude", position.getLongitude());
        queryAddPosition.setDouble("speed", position.getSpeed());
        queryAddPosition.setDouble("course", position.getCourse());
        queryAddPosition.setDouble("power", position.getPower());
        queryAddPosition.setInt("mode", position.getMode());
        queryAddPosition.setString("address", position.getAddress());
        queryAddPosition.setString("extended_info", position.getExtendedInfo());

        queryAddPosition.executeUpdate();

        // TODO: probably return row id
    }

}
