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
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.traccar.helper.DriverDelegate;
import org.traccar.model.Device;
import org.traccar.model.Permission;
import org.traccar.model.Position;
import org.traccar.model.User;

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

        params.setString("protocol", position.getProtocol());
        params.setLong("device_id", position.getDeviceId());
        params.setTimestamp("time", position.getFixTime());
        params.setBoolean("valid", position.getValid());
        params.setDouble("altitude", position.getAltitude());
        params.setDouble("latitude", position.getLatitude());
        params.setDouble("longitude", position.getLongitude());
        params.setDouble("speed", position.getSpeed());
        params.setDouble("course", position.getCourse());
        params.setString("address", position.getAddress());
        params.setString("extended_info", position.getOther());
        params.setLong("power", null); // temporary

        return params;
    }

    private void createDatabaseSchema() throws SQLException {

        Connection connection = dataSource.getConnection();
        ResultSet result = connection.getMetaData().getTables(
                connection.getCatalog(), null, null, null);
        
        boolean exist = false;
        while (result.next()) {
            if (result.getString("TABLE_NAME").equalsIgnoreCase("traccar1")) {
                exist = true;
                break;
            }
        }
        if (exist) {
            return;
        }
        
        QueryBuilder.create(dataSource,
                "CREATE TABLE user (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "name VARCHAR(1024) NOT NULL," +
                "email VARCHAR(1024) NOT NULL UNIQUE," +
                "password VARCHAR(1024) NOT NULL," +
                "salt VARCHAR(1024) NOT NULL," +
                "readonly BOOLEAN DEFAULT false NOT NULL," +
                "admin BOOLEAN DEFAULT false NOT NULL," +
                "map VARCHAR(1024) DEFAULT 'osm' NOT NULL," +
                "language VARCHAR(1024) DEFAULT 'en' NOT NULL," +
                "distanceUnit VARCHAR(1024) DEFAULT 'km' NOT NULL," +
                "speedUnit VARCHAR(1024) DEFAULT 'kmh' NOT NULL," +
                "latitude DOUBLE DEFAULT 0 NOT NULL," +
                "longitude DOUBLE DEFAULT 0 NOT NULL," +
                "zoom INT DEFAULT 0 NOT NULL);" +

                "CREATE TABLE device (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "name VARCHAR(1024) NOT NULL," +
                "uniqueId VARCHAR(1024) NOT NULL UNIQUE," +
                "status VARCHAR(1024)," +
                "lastUpdate TIMESTAMP," +
                "positionId INT," +
                "dataId INT);" +

                "CREATE TABLE user_device (" +
                "userId INT NOT NULL," +
                "deviceId INT NOT NULL," +
                "read BOOLEAN DEFAULT true NOT NULL," +
                "write BOOLEAN DEFAULT true NOT NULL," +
                "FOREIGN KEY (userId) REFERENCES user(id) ON DELETE CASCADE," +
                "FOREIGN KEY (deviceId) REFERENCES device(id) ON DELETE CASCADE);" +

                "CREATE INDEX user_device_user_id ON user_device(userId);" +

                "CREATE TABLE position (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "protocol VARCHAR(1024)," +
                "deviceId INT NOT NULL," +
                "serverTime TIMESTAMP NOT NULL," +
                "deviceTime TIMESTAMP NOT NULL," +
                "fixTime TIMESTAMP NOT NULL," +
                "valid BOOLEAN NOT NULL," +
                "latitude DOUBLE NOT NULL," +
                "longitude DOUBLE NOT NULL," +
                "altitude DOUBLE NOT NULL," +
                "speed DOUBLE NOT NULL," +
                "course DOUBLE NOT NULL," +
                "address VARCHAR(1024)," +
                "other VARCHAR(8192) NOT NULL," +
                "FOREIGN KEY (deviceId) REFERENCES device(id) ON DELETE CASCADE);" +

                "CREATE TABLE data (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "protocol VARCHAR(1024)," +
                "deviceId INT NOT NULL," +
                "serverTime TIMESTAMP NOT NULL," +
                "deviceTime TIMESTAMP NOT NULL," +
                "other VARCHAR(8192) NOT NULL," +
                "FOREIGN KEY (deviceId) REFERENCES device(id));" +

                "ALTER TABLE device ADD " +
                "FOREIGN KEY (positionId) REFERENCES position(id);" +

                "ALTER TABLE device ADD " +
                "FOREIGN KEY (dataId) REFERENCES data(id);" +

                "CREATE TABLE server (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "registration BOOLEAN NOT NULL," +
                "latitude DOUBLE NOT NULL," +
                "longitude DOUBLE NOT NULL," +
                "zoom INT NOT NULL);" +

                "CREATE TABLE traccar1 (" +
                "id INT PRIMARY KEY AUTO_INCREMENT);").executeUpdate();
        
        User admin = new User();
        admin.setName("admin");
        admin.setEmail("admin");
        admin.setPassword("admin");
        addUser(admin);
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

    public void addUser(User user) throws SQLException {
        user.setId(QueryBuilder.create(dataSource,
                "INSERT INTO user (name, email, password, salt, admin) " +
                "VALUES (:name, :email, CAST(HASH('SHA256', STRINGTOUTF8(:password), 1000) AS VARCHAR), '', :admin);")
                .setObject(user)
                .executeUpdate());
    }

    public Collection<Permission> getPermissions() throws SQLException {
        return QueryBuilder.create(dataSource, 
                "SELECT userId, deviceId FROM user_device;")
                .executeQuery(new Permission());
    }

    public Collection<Device> getDevices(long userId) throws SQLException {
        return QueryBuilder.create(dataSource, 
                "SELECT * FROM device WHERE id IN (" +
                "SELECT deviceId FROM user_device WHERE userId = :userId);")
                .setLong("userId", userId)
                .executeQuery(new Device());
    }
    
    public void addDevice(Device device) throws SQLException {
        device.setId(QueryBuilder.create(dataSource,
                "INSERT INTO device (name, uniqueId) VALUES (:name, :uniqueId);")
                .setObject(device)
                .executeUpdate());
    }
    
    public void updateDevice(Device device) throws SQLException {
        QueryBuilder.create(dataSource,
                "UPDATE device SET name = :name, uniqueId = :uniqueId WHERE id = :id;")
                .setObject(device)
                .executeUpdate();
    }
    
    public void removeDevice(Device device) throws SQLException {
        QueryBuilder.create(dataSource,
                "DELETE FROM device WHERE id = :id;")
                .setObject(device)
                .executeUpdate();
    }
    
    public void linkDevice(long userId, long deviceId) throws SQLException {
        QueryBuilder.create(dataSource,
                "INSERT INTO user_device (userId, deviceId) VALUES (:userId, :deviceId);")
                .setLong("userId", userId)
                .setLong("deviceId", deviceId)
                .executeUpdate();
    }

}
