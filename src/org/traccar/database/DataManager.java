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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.traccar.Context;
import org.traccar.helper.DriverDelegate;
import org.traccar.helper.Log;
import org.traccar.http.JsonConverter;
import org.traccar.model.Device;
import org.traccar.model.Permission;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.model.User;

public class DataManager {

    private static final long DEFAULT_REFRESH_DELAY = 300;
    
    private final Properties properties;
    
    private DataSource dataSource;

    private final Map<String, Device> devices = new HashMap<String, Device>();
    private long devicesLastUpdate;
    private long devicesRefreshDelay;

    public DataManager(Properties properties) throws Exception {
        this.properties = properties;
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
    
    public DataSource getDataSource() {
        return dataSource;
    }

    private void initDatabase(Properties properties) throws Exception {
        
        String jndiName = properties.getProperty("database.jndi");

        if (jndiName != null) {

            dataSource = (DataSource) new InitialContext().lookup(jndiName);

        } else {

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
            String maxPoolSize = properties.getProperty("database.maxPoolSize");
            if (maxPoolSize != null) {
                ds.setMaxPoolSize(Integer.valueOf(maxPoolSize));
            }
            dataSource = ds;
        }

        if (Boolean.valueOf(properties.getProperty("web.new"))) {
            createDatabaseSchema();
        }
    }

    public Device getDeviceByUniqueId(String uniqueId) throws SQLException {

        if ((new Date().getTime() - devicesLastUpdate > devicesRefreshDelay) || !devices.containsKey(uniqueId)) {

            devices.clear();
            for (Device device : getAllDevices()) {
                devices.put(device.getUniqueId(), device);
            }
            devicesLastUpdate = new Date().getTime();
        }

        return devices.get(uniqueId);
    }

    // TODO: possibly remove this method
    public void updateLatestPosition(Position position) throws SQLException {
        QueryBuilder.create(dataSource, properties.getProperty("database.updateLatestPosition"))
            .setObject(position)
            .executeUpdate();
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
        
        QueryBuilder.create(dataSource, properties.getProperty("database.createSchema")).executeUpdate();
        
        User admin = new User();
        admin.setName("admin");
        admin.setEmail("admin");
        admin.setPassword("admin");
        admin.setAdmin(true);
        addUser(admin);
        
        Server server = new Server();
        server.setRegistration(true);
        QueryBuilder.create(dataSource,
                "INSERT INTO server (registration, latitude, longitude, zoom)" +
                "VALUES (:registration, :latitude, :longitude, :zoom);")
                .setObject(server)
                .executeUpdate();

        mockData(admin.getId());
    }

    private void mockData(long userId) {
        if (Boolean.valueOf(Context.getProps().getProperty("database.mock"))) {
            try {

                Device device = new Device();
                device.setName("test1");
                device.setUniqueId("123456789012345");
                addDevice(device);
                linkDevice(userId, device.getId());

                Position position = new Position();
                position.setDeviceId(device.getId());

                position.setTime(JsonConverter.parseDate("2015-05-22T12:00:01"));
                position.setLatitude(-36.8785803);
                position.setLongitude(174.7281713);
                addPosition(position);

                position.setTime(JsonConverter.parseDate("2015-05-22T12:00:02"));
                position.setLatitude(-36.8870932);
                position.setLongitude(174.7473116);
                addPosition(position);

                position.setTime(JsonConverter.parseDate("2015-05-22T12:00:03"));
                position.setLatitude(-36.8932371);
                position.setLongitude(174.7743053);
                addPosition(position);

            } catch (SQLException error) {
                Log.warning(error);
            } catch (ParseException error) {
                Log.warning(error);
            }
        }
    }

    public User login(String email, String password) throws SQLException {
        return QueryBuilder.create(dataSource,
                "SELECT * FROM user WHERE email = :email AND " +
                "password = CAST(HASH('SHA256', STRINGTOUTF8(:password), 1000) AS VARCHAR);")
                .setString("email", email)
                .setString("password", password)
                .executeQuerySingle(new User());
    }

    public void addUser(User user) throws SQLException {
        user.setId(QueryBuilder.create(dataSource,
                "INSERT INTO user (name, email, password, salt, admin) " +
                "VALUES (:name, :email, CAST(HASH('SHA256', STRINGTOUTF8(:password), 1000) AS VARCHAR), '', :admin);")
                .setObject(user)
                .executeUpdate());
    }
    
    public void updateUser(User user) throws SQLException {
        QueryBuilder.create(dataSource,
                "UPDATE user SET name = :name, email = :email, admin = :admin," +
                "password = CASEWHEN((SELECT password FROM user WHERE id = :id) = :password, :password, CAST(HASH('SHA256', STRINGTOUTF8(:password), 1000) AS VARCHAR)) WHERE id = :id;")
                .setObject(user)
                .executeUpdate();
    }

    public Collection<Permission> getPermissions() throws SQLException {
        return QueryBuilder.create(dataSource, 
                "SELECT userId, deviceId FROM user_device;")
                .executeQuery(new Permission());
    }

    public Collection<Device> getAllDevices() throws SQLException {
        return QueryBuilder.create(dataSource, properties.getProperty("database.selectDeviceAll"))
                .executeQuery(new Device());
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

    public Collection<Position> getPositions(long userId, long deviceId, Date from, Date to) throws SQLException {
        return QueryBuilder.create(dataSource,
                "SELECT * FROM position WHERE deviceId = :deviceId AND fixTime BETWEEN :from AND :to;")
                .setLong("deviceId", deviceId)
                .setDate("from", from)
                .setDate("to", to)
                .executeQuery(new Position());
    }

    public void addPosition(Position position) throws SQLException {
        position.setId(QueryBuilder.create(dataSource, properties.getProperty("database.insertPosition"))
                .setObject(position)
                .setDate("time", position.getFixTime()) // tmp
                .setLong("device_id", position.getDeviceId()) // tmp
                .setLong("power", 0) // tmp
                .setString("extended_info", position.getOther()) // tmp
                .executeUpdate());
    }
    
    public Server getServer() throws SQLException {
        return QueryBuilder.create(dataSource,
                "SELECT * FROM server;")
                .executeQuerySingle(new Server());
    }
    
    public void updateServer(Server server) throws SQLException {
        QueryBuilder.create(dataSource,
                "UPDATE server SET registration = :registration WHERE id = :id;")
                .setObject(server)
                .executeUpdate();
    }

}
