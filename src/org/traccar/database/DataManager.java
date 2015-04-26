/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
import javax.json.JsonArray;
import javax.sql.DataSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.traccar.helper.DriverDelegate;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.xml.sax.InputSource;

public class DataManager {

    public DataManager(Properties properties) throws Exception {
        if (properties != null) {
            initDatabase(properties);
            
            // Refresh delay
            String refreshDelay = properties.getProperty("database.refreshDelay");
            if (refreshDelay != null) {
                devicesRefreshDelay = Long.valueOf(refreshDelay) * 1000;
            } else {
                devicesRefreshDelay = DEFAULT_REFRESH_DELAY * 1000;
            }
        }
    }
    
    private DataSource dataSource;
    
    public DataSource getDataSource() {
        return dataSource;
    }

    private NamedParameterStatement queryGetDevices;
    private NamedParameterStatement queryAddPosition;
    private NamedParameterStatement queryUpdateLatestPosition;
    
    private boolean useNewDatabase;

    private void initDatabase(Properties properties) throws Exception {
        
        useNewDatabase = Boolean.valueOf(properties.getProperty("http.new"));

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
        
        if (useNewDatabase) {
            createDatabaseSchema();
        }

        // Load statements from configuration
        String query;

        query = properties.getProperty("database.selectDevice");
        if (query != null) {
            queryGetDevices = new NamedParameterStatement(query, dataSource);
        }

        query = properties.getProperty("database.insertPosition");
        if (query != null) {
            queryAddPosition = new NamedParameterStatement(query, dataSource, Statement.RETURN_GENERATED_KEYS);
        }

        query = properties.getProperty("database.updateLatestPosition");
        if (query != null) {
            queryUpdateLatestPosition = new NamedParameterStatement(query, dataSource);
        }
    }

    private final NamedParameterStatement.ResultSetProcessor<Device> deviceResultSetProcessor = new NamedParameterStatement.ResultSetProcessor<Device>() {
        @Override
        public Device processNextRow(ResultSet rs) throws SQLException {
            Device device = new Device();
            device.setId(rs.getLong("id"));
            device.setUniqueId(rs.getString("imei"));
            return device;
        }
    };

    public List<Device> getDevices() throws SQLException {
        if (queryGetDevices != null) {
            return queryGetDevices.prepare().executeQuery(deviceResultSetProcessor);
        } else {
            return new LinkedList<Device>();
        }
    }

    /**
     * Devices cache
     */
    private Map<String, Device> devices;
    private Calendar devicesLastUpdate;
    private long devicesRefreshDelay;
    private static final long DEFAULT_REFRESH_DELAY = 300;

    public Device getDeviceByUniqueId(String uniqueId) throws SQLException {

        if (devices == null || !devices.containsKey(uniqueId) ||
                (Calendar.getInstance().getTimeInMillis() - devicesLastUpdate.getTimeInMillis() > devicesRefreshDelay)) {

            devices = new HashMap<String, Device>();
            for (Device device : getDevices()) {
                devices.put(device.getUniqueId(), device);
            }
            devicesLastUpdate = Calendar.getInstance();
        }

        return devices.get(uniqueId);
    }

    private NamedParameterStatement.ResultSetProcessor<Long> generatedKeysResultSetProcessor = new NamedParameterStatement.ResultSetProcessor<Long>() {
        @Override
        public Long processNextRow(ResultSet rs) throws SQLException {
            return rs.getLong(1);
        }
    };

    public synchronized Long addPosition(Position position) throws SQLException {
        if (queryAddPosition != null) {
            List<Long> result = assignVariables(queryAddPosition.prepare(), position).executeUpdate(generatedKeysResultSetProcessor);
            if (result != null && !result.isEmpty()) {
                return result.iterator().next();
            }
        }
        return null;
    }

    public void updateLatestPosition(Position position, Long positionId) throws SQLException {
        if (queryUpdateLatestPosition != null) {
            assignVariables(queryUpdateLatestPosition.prepare(), position).setLong("id", positionId).executeUpdate();
        }
    }

    private NamedParameterStatement.Params assignVariables(NamedParameterStatement.Params params, Position position) throws SQLException {

        params.setLong("device_id", position.getDeviceId());
        params.setTimestamp("time", position.getTime());
        params.setBoolean("valid", position.getValid());
        params.setDouble("altitude", position.getAltitude());
        params.setDouble("latitude", position.getLatitude());
        params.setDouble("longitude", position.getLongitude());
        params.setDouble("speed", position.getSpeed());
        params.setDouble("course", position.getCourse());
        params.setString("address", position.getAddress());
        params.setString("extended_info", position.getExtendedInfo());

        // DELME: Temporary compatibility support
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            InputSource source = new InputSource(new StringReader(position.getExtendedInfo()));
            String index = xpath.evaluate("/info/index", source);
            if (!index.isEmpty()) {
                params.setLong("id", Long.valueOf(index));
            } else {
                params.setLong("id", null);
            }
            source = new InputSource(new StringReader(position.getExtendedInfo()));
            String power = xpath.evaluate("/info/power", source);
            if (!power.isEmpty()) {
                params.setDouble("power", Double.valueOf(power));
            } else {
                params.setLong("power", null);
            }
        } catch (XPathExpressionException e) {
            Log.warning("Error in XML: " + position.getExtendedInfo(), e);
            params.setLong("id", null);
            params.setLong("power", null);
        }

        return params;
    }

    private void createDatabaseSchema() throws SQLException {

        Connection connection = dataSource.getConnection();
        try {
            Statement statement = connection.createStatement();
            try {
                
                ResultSet result = connection.getMetaData().getTables(
                        connection.getCatalog(), null, null, null);
                
                boolean exist = false;
                while (result.next()) {
                    if (result.getString("TABLE_NAME").equalsIgnoreCase("traccar1")) {
                        exist = true;
                        break;
                    }
                }
                
                if (!exist) {

                    statement.execute(
                            "CREATE TABLE user (" +
                            "id INT PRIMARY KEY AUTO_INCREMENT," +
                            "email VARCHAR(1024) NOT NULL UNIQUE," +
                            "password VARCHAR(1024) NOT NULL," +
                            "salt VARCHAR(1024) NOT NULL," +
                            "readonly BOOLEAN DEFAULT false NOT NULL," +
                            "admin BOOLEAN DEFAULT false NOT NULL," +
                            "map VARCHAR(1024) DEFAULT 'osm' NOT NULL," +
                            "language VARCHAR(1024) DEFAULT 'en' NOT NULL," +
                            "distance_unit VARCHAR(1024) DEFAULT 'km' NOT NULL," +
                            "speed_unit VARCHAR(1024) DEFAULT 'kmh' NOT NULL," +
                            "latitude DOUBLE DEFAULT 0 NOT NULL," +
                            "longitude DOUBLE DEFAULT 0 NOT NULL," +
                            "zoom INT DEFAULT 0 NOT NULL);" +
                                    
                            "CREATE TABLE device (" +
                            "id INT PRIMARY KEY AUTO_INCREMENT," +
                            "name VARCHAR(1024) NOT NULL," +
                            "unique_id VARCHAR(1024) NOT NULL UNIQUE," +
                            "status VARCHAR(1024)," +
                            "last_update TIMESTAMP," +
                            "position_id INT," +
                            "data_id INT);" +

                            "CREATE TABLE user_device (" +
                            "user_id INT NOT NULL," +
                            "device_id INT NOT NULL," +
                            "read BOOLEAN DEFAULT true NOT NULL," +
                            "write BOOLEAN DEFAULT true NOT NULL," +
                            "FOREIGN KEY (user_id) REFERENCES user(id)," +
                            "FOREIGN KEY (device_id) REFERENCES device(id));" +

                            "CREATE INDEX user_device_user_id ON user_device(user_id);" +

                            "CREATE TABLE position (" +
                            "id INT PRIMARY KEY AUTO_INCREMENT," +
                            "device_id INT NOT NULL," +
                            "server_time TIMESTAMP NOT NULL," +
                            "device_time TIMESTAMP NOT NULL," +
                            "fix_time TIMESTAMP NOT NULL," +
                            "valid BOOLEAN NOT NULL," +
                            "latitude DOUBLE NOT NULL," +
                            "longitude DOUBLE NOT NULL," +
                            "altitude DOUBLE NOT NULL," +
                            "speed DOUBLE NOT NULL," +
                            "course DOUBLE NOT NULL," +
                            "address VARCHAR(1024)," +
                            "other VARCHAR(8192) NOT NULL," +
                            "FOREIGN KEY (device_id) REFERENCES device(id) ON DELETE CASCADE);" +

                            "CREATE TABLE data (" +
                            "id INT PRIMARY KEY AUTO_INCREMENT," +
                            "device_id INT NOT NULL," +
                            "server_time TIMESTAMP NOT NULL," +
                            "device_time TIMESTAMP NOT NULL," +
                            "other VARCHAR(8192) NOT NULL," +
                            "FOREIGN KEY (device_id) REFERENCES device(id));" +

                            "ALTER TABLE device ADD " +
                            "FOREIGN KEY (position_id) REFERENCES position(id);" +

                            "ALTER TABLE device ADD " +
                            "FOREIGN KEY (data_id) REFERENCES data(id);" +

                            "CREATE TABLE server (" +
                            "id INT PRIMARY KEY AUTO_INCREMENT," +
                            "registration BOOLEAN NOT NULL," +
                            "latitude DOUBLE NOT NULL," +
                            "longitude DOUBLE NOT NULL," +
                            "zoom INT NOT NULL);" +

                            "CREATE TABLE traccar1 (" +
                            "id INT PRIMARY KEY AUTO_INCREMENT);");
                    
                    addUser("admin", "admin", true);
                }

            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
    }

    public long login(String email, String password) throws SQLException {

        Connection connection = dataSource.getConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM user WHERE email = ? AND " +
                    "password = CAST(HASH('SHA256', STRINGTOUTF8(?), 1000) AS VARCHAR);");
            try {
                statement.setString(1, email);
                statement.setString(2, password);

                ResultSet result = statement.executeQuery();
                result.next();
                return result.getLong("id");
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
    }

    public void addUser(String email, String password, boolean admin) throws SQLException {

        Connection connection = dataSource.getConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO user (email, password, salt, admin) " +
                    "VALUES (?, CAST(HASH('SHA256', STRINGTOUTF8(?), 1000) AS VARCHAR), '', ?);");
            try {
                statement.setString(1, email);
                statement.setString(2, password);
                statement.setBoolean(3, admin);
                
                statement.executeUpdate();
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
    }
    
    public List<Long> getDeviceList(long userId) throws SQLException {

        Connection connection = dataSource.getConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM device WHERE id IN (" +
                    "SELECT device_id FROM user_device WHERE user_id = ?);");
            try {
                statement.setLong(1, userId);

                ResultSet resultSet = statement.executeQuery();
                
                List<Long> result = new LinkedList<Long>();
                while (resultSet.next()) {
                    result.add(resultSet.getLong(1));
                }
                return result;
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
    }
    
    public JsonArray getDevices(long userId) throws SQLException {

        Connection connection = dataSource.getConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM device WHERE id IN (" +
                    "SELECT device_id FROM user_device WHERE user_id = ?);");
            try {
                statement.setLong(1, userId);
                
                return ResultSetConverter.convert(statement.executeQuery());
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
    }
    
    public void addDevice(Device device) throws SQLException {

        Connection connection = dataSource.getConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO device (name, unique_id) VALUES (?, ?);",
                    Statement.RETURN_GENERATED_KEYS);
            try {
                statement.setString(1, device.getName());
                statement.setString(2, device.getUniqueId());
                
                statement.execute();
                
                ResultSet result = statement.getGeneratedKeys();
                if (result.next()) {
                    device.setId(result.getLong(1));
                }
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
    }
    
    public void updateDevice(Device device) throws SQLException {

        Connection connection = dataSource.getConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE device SET name = ?, unique_id = ? WHERE id = ?;");
            try {
                statement.setString(1, device.getName());
                statement.setString(2, device.getUniqueId());
                statement.setLong(3, device.getId());
                
                statement.execute();
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
    }
    
    public void removeDevice(Device device) throws SQLException {

        Connection connection = dataSource.getConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM device WHERE id = ?;");
            try {
                statement.setLong(1, device.getId());
                
                statement.execute();
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
    }

}
