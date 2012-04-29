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
    private NamedParameterStatement queryAddPosition;

    /**
     * Devices cache
     */
    private Map devices;
    private Calendar devicesLastUpdate;
    private Long devicesRefreshDelay;

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

        String query = properties.getProperty("database.selectDevice");
        queryGetDevices = new NamedParameterStatement(connection, query);

        query = properties.getProperty("database.insertPosition");
        queryAddPosition = new NamedParameterStatement(connection, query);
    }

    public synchronized List<Device> getDevices() throws SQLException {

        List<Device> deviceList = new LinkedList();

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

    public void addDevice(Device device) {} // TODO: implement
    public void addUpdate(Device device) {} // TODO: implement
    public void addRemove(Device device) {} // TODO: implement

    public Device getDeviceByImei(String imei) throws SQLException {

        if ((devices == null) || (Calendar.getInstance().getTimeInMillis() - devicesLastUpdate.getTimeInMillis() > devicesRefreshDelay)) {
            devices = new HashMap();
            for (Device device: getDevices()) {
                devices.put(device.getImei(), device);
            }
            devicesLastUpdate = Calendar.getInstance();
        }

        return (Device) devices.get(imei);
    }

    public List<Position> getPositions(Long deviceId) { // TODO: implement

        List<Position> positionList = new LinkedList();

        Position p = new Position();
        p.setDeviceId(new Long(1));
        p.setTime(new Date());
        p.setValid(true);
        p.setLatitude(1.0);
        p.setLongitude(1.0);
        p.setSpeed(1.0);
        p.setCourse(1.0);

        positionList.add(p);

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
        queryAddPosition.setString("extended_info", position.getExtendedInfo());

        queryAddPosition.executeUpdate();
    }

}
