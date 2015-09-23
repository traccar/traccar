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
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.traccar.Config;
import org.traccar.helper.DriverDelegate;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.MiscFormatter;
import org.traccar.model.Permission;
import org.traccar.model.Position;
import org.traccar.model.Schema;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.web.AsyncServlet;
import org.traccar.web.JsonConverter;

public class DataManager implements IdentityManager {

    private static final long DEFAULT_REFRESH_DELAY = 300;
    
    private final Config config;
    
    private DataSource dataSource;
    
    private final Map<Long, Device> devicesById = new HashMap<>();
    private final Map<String, Device> devicesByUniqueId = new HashMap<>();
    private long devicesLastUpdate;
    private final long devicesRefreshDelay;

    public DataManager(Config config) throws Exception {
        this.config = config;

        initDatabase();
        initDatabaseSchema();

        devicesRefreshDelay = config.getLong("database.refreshDelay", DEFAULT_REFRESH_DELAY) * 1000;
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }

    private void initDatabase() throws Exception {
        
        String jndiName = config.getString("database.jndi");

        if (jndiName != null) {

            dataSource = (DataSource) new InitialContext().lookup(jndiName);

        } else {

            // Load driver
            String driver = config.getString("database.driver");
            if (driver != null) {
                String driverFile = config.getString("database.driverFile");

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
            ds.setDriverClass(config.getString("database.driver"));
            ds.setJdbcUrl(config.getString("database.url"));
            ds.setUser(config.getString("database.user"));
            ds.setPassword(config.getString("database.password"));
            ds.setIdleConnectionTestPeriod(600);
            ds.setTestConnectionOnCheckin(true);
            int maxPoolSize = config.getInteger("database.maxPoolSize");
            if (maxPoolSize != 0) {
                ds.setMaxPoolSize(maxPoolSize);
            }
            dataSource = ds;
        }
    }
    
    @Override
    public Device getDeviceById(long id) {
        return devicesById.get(id);
    }

    @Override
    public Device getDeviceByUniqueId(String uniqueId) throws SQLException {

        if ((new Date().getTime() - devicesLastUpdate > devicesRefreshDelay) || !devicesByUniqueId.containsKey(uniqueId)) {

            devicesById.clear();
            devicesByUniqueId.clear();
            for (Device device : getAllDevices()) {
                devicesById.put(device.getId(), device);
                devicesByUniqueId.put(device.getUniqueId(), device);
            }
            devicesLastUpdate = new Date().getTime();
        }

        return devicesByUniqueId.get(uniqueId);
    }
    
    private String getQuery(String key) {
        String query = config.getString(key);
        if (query == null) {
            Log.info("Query not provided: " + key);
        }
        return query;
    }

    private void initDatabaseSchema() throws SQLException {

        if (config.getString("web.type", "new").equals("new") || config.getString("web.type", "new").equals("api")) {

            Connection connection = dataSource.getConnection();
            ResultSet result = connection.getMetaData().getTables(
                    connection.getCatalog(), null, null, null);

            boolean exist = false;
            String checkTable = config.getString("database.checkTable");
            while (result.next()) {
                if (result.getString("TABLE_NAME").equalsIgnoreCase(checkTable)) {
                    exist = true;
                    break;
                }
            }
            if (exist) {
                
                String schemaVersionQuery = getQuery("database.selectSchemaVersion");
                if (schemaVersionQuery != null) {
                
                    Schema schema = QueryBuilder.create(dataSource, schemaVersionQuery).executeQuerySingle(new Schema());

                    int version = 0;
                    if (schema != null) {
                        version = schema.getVersion();
                    }

                    if (version != 301) {
                        Log.error("Wrong database schema version (" + version + ")");
                        throw new RuntimeException();
                    }
                }
                
                return;
            }

            QueryBuilder.create(dataSource, getQuery("database.createSchema")).executeUpdate();

            User admin = new User();
            admin.setName("admin");
            admin.setEmail("admin");
            admin.setAdmin(true);
            admin.setPassword("admin");
            addUser(admin);

            Server server = new Server();
            server.setRegistration(true);
            QueryBuilder.create(dataSource, getQuery("database.insertServer"))
                    .setObject(server)
                    .executeUpdate();

            mockData(admin.getId());
        }
    }
    
    private void mockData(long userId) {
        if (config.getBoolean("database.mock")) {
            try {

                Device device = new Device();
                device.setName("test1");
                device.setUniqueId("123456789012345");
                addDevice(device);
                linkDevice(userId, device.getId());

                Position position = new Position();
                position.setDeviceId(device.getId());

                position.setTime(JsonConverter.parseDate("2015-05-22T12:00:01.000Z"));
                position.setLatitude(-36.8785803);
                position.setLongitude(174.7281713);
                addPosition(position);

                position.setTime(JsonConverter.parseDate("2015-05-22T12:00:02.000Z"));
                position.setLatitude(-36.8870932);
                position.setLongitude(174.7473116);
                addPosition(position);

                position.setTime(JsonConverter.parseDate("2015-05-22T12:00:03.000Z"));
                position.setLatitude(-36.8932371);
                position.setLongitude(174.7743053);
                addPosition(position);
                
                updateLatestPosition(position);

            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }

    public User login(String email, String password) throws SQLException {
        User user = QueryBuilder.create(dataSource, getQuery("database.loginUser"))
                .setString("email", email)
                .executeQuerySingle(new User());
        return user != null && user.isPasswordValid(password) ? user : null;
    }

    public Collection<User> getUsers() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectUsersAll"))
                .executeQuery(new User());
    }

    public User getUser(long userId) throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectUser"))
                .setLong("id", userId)
                .executeQuerySingle(new User());
    }

    public void addUser(User user) throws SQLException {
        user.setId(QueryBuilder.create(dataSource, getQuery("database.insertUser"), true)
                .setObject(user)
                .executeUpdate());
    }
    
    public void updateUser(User user) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.updateUser"))
                .setObject(user)
                .executeUpdate();
        if (user.getHashedPassword() != null) {
            QueryBuilder.create(dataSource, getQuery("database.updateUserPassword"))
                .setObject(user)
                .executeUpdate();
        }
    }

    public void removeUser(User user) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.deleteUser"))
                .setObject(user)
                .executeUpdate();
    }

    public Collection<Permission> getPermissions() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.getPermissionsAll"))
                .executeQuery(new Permission());
    }

    public Collection<Device> getAllDevices() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectDevicesAll"))
                .executeQuery(new Device());
    }

    public Collection<Device> getDevices(long userId) throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectDevices"))
                .setLong("userId", userId)
                .executeQuery(new Device());
    }
    
    public void addDevice(Device device) throws SQLException {
        device.setId(QueryBuilder.create(dataSource, getQuery("database.insertDevice"), true)
                .setObject(device)
                .executeUpdate());
    }
    
    public void updateDevice(Device device) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.updateDevice"))
                .setObject(device)
                .executeUpdate();
    }
    
    public void removeDevice(Device device) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.deleteDevice"))
                .setObject(device)
                .executeUpdate();
        AsyncServlet.sessionRefreshDevice(device.getId());
    }
    
    public void linkDevice(long userId, long deviceId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.linkDevice"))
                .setLong("userId", userId)
                .setLong("deviceId", deviceId)
                .executeUpdate();
        AsyncServlet.sessionRefreshUser(userId);
    }

    public Collection<Position> getPositions(long userId, long deviceId, Date from, Date to) throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectPositions"))
                .setLong("deviceId", deviceId)
                .setDate("from", from)
                .setDate("to", to)
                .executeQuery(new Position());
    }

    public void addPosition(Position position) throws SQLException {
        position.setId(QueryBuilder.create(dataSource, getQuery("database.insertPosition"), true)
                .setObject(position)
                .setDate("time", position.getFixTime()) // tmp
                .setLong("device_id", position.getDeviceId()) // tmp
                .setLong("power", 0) // tmp
                .setString("extended_info", MiscFormatter.toXmlString(position.getAttributes())) // tmp
                .setString("other", MiscFormatter.toXmlString(position.getAttributes())) // tmp
                .executeUpdate());
    }

    public void updateLatestPosition(Position position) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.updateLatestPosition"))
                .setObject(position)
                .setDate("time", position.getFixTime()) // tmp
                .setLong("device_id", position.getDeviceId()) // tmp
                .setLong("power", 0) // tmp
                .setString("extended_info", MiscFormatter.toXmlString(position.getAttributes())) // tmp
                .setString("other", MiscFormatter.toXmlString(position.getAttributes())) // tmp
                .executeUpdate();
    }

    public Collection<Position> getLatestPositions() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectLatestPositions"))
                .executeQuery(new Position());
    }

    public Server getServer() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectServers"))
                .executeQuerySingle(new Server());
    }

    public void updateServer(Server server) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.updateServer"))
                .setObject(server)
                .executeUpdate();
    }

}
