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
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.traccar.Config;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.MiscFormatter;
import org.traccar.model.Permission;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.web.AsyncServlet;

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

            String driverFile = config.getString("database.driverFile");
            if (driverFile != null) {
                URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(classLoader, new File(driverFile).toURI().toURL());
            }

            String driver = config.getString("database.driver");
            if (driver != null) {
                Class.forName(driver);
            }

            ComboPooledDataSource ds = new ComboPooledDataSource();
            ds.setDriverClass(config.getString("database.driver"));
            ds.setJdbcUrl(config.getString("database.url"));
            ds.setUser(config.getString("database.user"));
            ds.setPassword(config.getString("database.password"));
            ds.setIdleConnectionTestPeriod(600);
            ds.setTestConnectionOnCheckin(true);
            ds.setMaxStatementsPerConnection(config.getInteger("database.maxStatements"));
            int maxPoolSize = config.getInteger("database.maxPoolSize");
            if (maxPoolSize != 0) {
                ds.setMaxPoolSize(maxPoolSize);
            }
            dataSource = ds;

        }
    }

    private void updateDeviceCache(boolean force) throws SQLException {
        if (System.currentTimeMillis() - devicesLastUpdate > devicesRefreshDelay || force) {
            devicesById.clear();
            devicesByUniqueId.clear();
            for (Device device : getAllDevices()) {
                devicesById.put(device.getId(), device);
                devicesByUniqueId.put(device.getUniqueId(), device);
            }
            devicesLastUpdate = System.currentTimeMillis();
        }
    }

    @Override
    public Device getDeviceById(long id) {
        try {
            updateDeviceCache(!devicesById.containsKey(id));
        } catch (SQLException e) {
            Log.warning(e);
        }
        return devicesById.get(id);
    }

    @Override
    public Device getDeviceByUniqueId(String uniqueId) throws SQLException {
        updateDeviceCache(
                !devicesByUniqueId.containsKey(uniqueId) && !config.getBoolean("database.ignoreUnknown"));
        return devicesByUniqueId.get(uniqueId);
    }

    private String getQuery(String key) {
        String query = config.getString(key);
        if (query == null) {
            Log.info("Query not provided: " + key);
        }
        return query;
    }

    private void initDatabaseSchema() throws SQLException, LiquibaseException {

        if (config.hasKey("database.changelog")) {

            ResourceAccessor resourceAccessor = new FileSystemResourceAccessor();

            Database database = DatabaseFactory.getInstance().openDatabase(
                    config.getString("database.url"),
                    config.getString("database.user"),
                    config.getString("database.password"),
                    null, resourceAccessor);

            Liquibase liquibase = new Liquibase(
                    config.getString("database.changelog"), resourceAccessor, database);

            liquibase.clearCheckSums();

            liquibase.update(new Contexts());
        }
    }

    public User login(String email, String password) throws SQLException {
        User user = QueryBuilder.create(dataSource, getQuery("database.loginUser"))
                .setString("email", email)
                .executeQuerySingle(User.class);
        if (user != null && user.isPasswordValid(password)) {
            return user;
        } else {
            return null;
        }
    }

    public Collection<User> getUsers() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectUsersAll"))
                .executeQuery(User.class);
    }

    public User getUser(long userId) throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectUser"))
                .setLong("id", userId)
                .executeQuerySingle(User.class);
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

    @Deprecated
    public void removeUser(User user) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.deleteUser"))
                .setObject(user)
                .executeUpdate();
    }

    public void removeUser(long userId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.deleteUser"))
                .setLong("id", userId)
                .executeUpdate();
    }

    public Collection<Permission> getPermissions() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.getPermissionsAll"))
                .executeQuery(Permission.class);
    }

    public Collection<Device> getAllDevices() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectDevicesAll"))
                .executeQuery(Device.class);
    }

    public Collection<Device> getDevices(long userId) throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectDevices"))
                .setLong("userId", userId)
                .executeQuery(Device.class);
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

    public void updateDeviceStatus(Device device) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.updateDeviceStatus"))
                .setObject(device)
                .executeUpdate();
    }

    @Deprecated
    public void removeDevice(Device device) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.deleteDevice"))
                .setObject(device)
                .executeUpdate();
        AsyncServlet.sessionRefreshDevice(device.getId());
    }

    public void removeDevice(long deviceId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.deleteDevice"))
                .setLong("id", deviceId)
                .executeUpdate();
        AsyncServlet.sessionRefreshDevice(deviceId);
    }

    public void linkDevice(long userId, long deviceId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.linkDevice"))
                .setLong("userId", userId)
                .setLong("deviceId", deviceId)
                .executeUpdate();
        AsyncServlet.sessionRefreshUser(userId);
    }

    public void unlinkDevice(long userId, long deviceId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.unlinkDevice"))
                .setLong("userId", userId)
                .setLong("deviceId", deviceId)
                .executeUpdate();
        AsyncServlet.sessionRefreshUser(userId);
    }

    public Collection<Position> getPositions(long deviceId, Date from, Date to) throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectPositions"))
                .setLong("deviceId", deviceId)
                .setDate("from", from)
                .setDate("to", to)
                .executeQuery(Position.class);
    }

    public void addPosition(Position position) throws SQLException {
        position.setId(QueryBuilder.create(dataSource, getQuery("database.insertPosition"), true)
                .setDate("now", new Date())
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
                .setDate("now", new Date())
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
                .executeQuery(Position.class);
    }

    public Server getServer() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectServers"))
                .executeQuerySingle(Server.class);
    }

    public void updateServer(Server server) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.updateServer"))
                .setObject(server)
                .executeUpdate();
    }

}
