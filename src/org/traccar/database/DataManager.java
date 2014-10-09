/*
 * Copyright 2012 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.traccar.helper.DriverDelegate;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.xml.sax.InputSource;

/**
 * Database abstraction class
 */
public class DataManager {

    public DataManager(Properties properties) throws Exception {
        if (properties != null) {
            initDatabase(properties);
        }
    }
    
    private DataSource dataSource;
    
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Database statements
     */
    private NamedParameterStatement queryGetDevices;
    private NamedParameterStatement queryAddPosition;
    private NamedParameterStatement queryUpdateLatestPosition;

    /**
     * Initialize database
     */
    private void initDatabase(Properties properties) throws Exception {

        // Load driver
        String driver = properties.getProperty("database.driver");
        if (driver != null) {
            String driverFile = properties.getProperty("database.driverFile");

            if (driverFile != null) {
                URL url = new URL("jar:file:" + new File(driverFile).getAbsolutePath() + "!/");
                URLClassLoader cl = new URLClassLoader(new URL[]{url});
                Driver d = (Driver) Class.forName(driver, true, cl).newInstance();
                DriverManager.registerDriver(new DriverDelegate(d));
            } else {
                Class.forName(driver);
            }
        }
        
        // Initialize data source
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setDriverClass(properties.getProperty("database.driver"));
        ds.setJdbcUrl(properties.getProperty("database.url"));
        ds.setUser(properties.getProperty("database.user"));
        ds.setPassword(properties.getProperty("database.password"));
        ds.setIdleConnectionTestPeriod(600);
        ds.setTestConnectionOnCheckin(true);
        dataSource = ds;

        // Load statements from configuration
        String query;

        query = properties.getProperty("database.selectDevice");
        if (query != null) {
            queryGetDevices = new NamedParameterStatement(query);
        }

        query = properties.getProperty("database.insertPosition");
        if (query != null) {
            queryAddPosition = new NamedParameterStatement(query);
        }

        query = properties.getProperty("database.updateLatestPosition");
        if (query != null) {
            queryUpdateLatestPosition = new NamedParameterStatement(query);
        }
    }

    public synchronized List<Device> getDevices() throws SQLException {

        List<Device> deviceList = new LinkedList<Device>();

        if (queryGetDevices != null) {
            Connection connection = dataSource.getConnection();
            try {
                PreparedStatement statement = queryGetDevices.prepare(connection);
                try {
                    ResultSet result = statement.executeQuery();
                    while (result.next()) {
                        Device device = new Device();
                        device.setId(result.getLong("id"));
                        device.setImei(result.getString("imei"));
                        deviceList.add(device);
                    }
                } finally {
                    statement.close();
                }
            } finally {
                connection.close();
            }
        }

        return deviceList;
    }

    /**
     * Devices cache
     */
    private Map<String, Device> devices;

    public Device getDeviceByImei(String imei) throws SQLException {

        if (devices == null || !devices.containsKey(imei)) {
            devices = new HashMap<String, Device>();
            for (Device device : getDevices()) {
                devices.put(device.getImei(), device);
            }
        }

        return devices.get(imei);
    }

    public synchronized Long addPosition(Position position) throws SQLException {

        if (queryAddPosition != null) {
            Connection connection = dataSource.getConnection();
            try {
                PreparedStatement statement = queryAddPosition.prepare(connection, Statement.RETURN_GENERATED_KEYS);
                try {
                    assignVariables(queryAddPosition, statement, position);
                    statement.executeUpdate();

                    ResultSet result = statement.getGeneratedKeys();
                    if (result != null && result.next()) {
                        return result.getLong(1);
                    }
                } finally {
                    statement.close();
                }
            } finally {
                connection.close();
            }
        }

        return null;
    }

    public void updateLatestPosition(Position position, Long positionId) throws SQLException {
        if (queryUpdateLatestPosition != null) {
            Connection connection = dataSource.getConnection();
            try {
                PreparedStatement statement = queryUpdateLatestPosition.prepare(connection);
                try {
                    assignVariables(queryUpdateLatestPosition, statement, position);
                    queryUpdateLatestPosition.setLong(statement, "id", positionId);
                    statement.executeUpdate();
                } finally {
                    statement.close();
                }
            } finally {
                connection.close();
            }
        }
    }

    private void assignVariables(NamedParameterStatement nps, PreparedStatement ps, Position position) throws SQLException {

        nps.setLong(ps, "device_id", position.getDeviceId());
        nps.setTimestamp(ps, "time", position.getTime());
        nps.setBoolean(ps, "valid", position.getValid());
        nps.setDouble(ps, "altitude", position.getAltitude());
        nps.setDouble(ps, "latitude", position.getLatitude());
        nps.setDouble(ps, "longitude", position.getLongitude());
        nps.setDouble(ps, "speed", position.getSpeed());
        nps.setDouble(ps, "course", position.getCourse());
        nps.setString(ps, "address", position.getAddress());
        nps.setString(ps, "extended_info", position.getExtendedInfo());

        // DELME: Temporary compatibility support
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            InputSource source = new InputSource(new StringReader(position.getExtendedInfo()));
            String index = xpath.evaluate("/info/index", source);
            if (!index.isEmpty()) {
                nps.setLong(ps, "id", Long.valueOf(index));
            } else {
                nps.setLong(ps, "id", null);
            }
            source = new InputSource(new StringReader(position.getExtendedInfo()));
            String power = xpath.evaluate("/info/power", source);
            if (!power.isEmpty()) {
                nps.setDouble(ps, "power", Double.valueOf(power));
            } else {
                nps.setLong(ps, "power", null);
            }
        } catch (XPathExpressionException e) {
            Log.warning("Error in XML: " + position.getExtendedInfo(), e);
            nps.setLong(ps, "id", null);
            nps.setLong(ps, "power", null);
        }
    }

}
