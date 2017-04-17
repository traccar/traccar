/*
 * Copyright 2012 - 2017 Anton Tananaev (anton@traccar.org)
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

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;

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
import org.traccar.model.AttributeAlias;
import org.traccar.model.Calendar;
import org.traccar.model.CalendarPermission;
import org.traccar.model.Device;
import org.traccar.model.DevicePermission;
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Group;
import org.traccar.model.GroupGeofence;
import org.traccar.model.GroupPermission;
import org.traccar.model.Notification;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.model.Statistics;
import org.traccar.model.User;
import org.traccar.model.UserPermission;
import org.traccar.model.DeviceGeofence;
import org.traccar.model.GeofencePermission;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataManager {

    private final Config config;

    private DataSource dataSource;

    public DataManager(Config config) throws Exception {
        this.config = config;

        initDatabase();
        initDatabaseSchema();
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

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(config.getString("database.driver"));
            hikariConfig.setJdbcUrl(config.getString("database.url"));
            hikariConfig.setUsername(config.getString("database.user"));
            hikariConfig.setPassword(config.getString("database.password"));
            hikariConfig.setConnectionInitSql(config.getString("database.checkConnection", "SELECT 1"));
            hikariConfig.setIdleTimeout(600000);

            int maxPoolSize = config.getInteger("database.maxPoolSize");

            if (maxPoolSize != 0) {
                hikariConfig.setMaximumPoolSize(maxPoolSize);
            }

            dataSource = new HikariDataSource(hikariConfig);

        }
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

    public void removeUser(long userId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.deleteUser"))
                .setLong("id", userId)
                .executeUpdate();
    }

    public Collection<DevicePermission> getDevicePermissions() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectDevicePermissions"))
                .executeQuery(DevicePermission.class);
    }

    public Collection<GroupPermission> getGroupPermissions() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectGroupPermissions"))
                .executeQuery(GroupPermission.class);
    }

    public Collection<Device> getAllDevices() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectDevicesAll"))
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

    public void removeDevice(long deviceId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.deleteDevice"))
                .setLong("id", deviceId)
                .executeUpdate();
    }

    public void linkDevice(long userId, long deviceId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.linkDevice"))
                .setLong("userId", userId)
                .setLong("deviceId", deviceId)
                .executeUpdate();
    }

    public void unlinkDevice(long userId, long deviceId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.unlinkDevice"))
                .setLong("userId", userId)
                .setLong("deviceId", deviceId)
                .executeUpdate();
    }

    public Collection<Group> getAllGroups() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectGroupsAll"))
                .executeQuery(Group.class);
    }

    public void addGroup(Group group) throws SQLException {
        group.setId(QueryBuilder.create(dataSource, getQuery("database.insertGroup"), true)
                .setObject(group)
                .executeUpdate());
    }

    public void updateGroup(Group group) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.updateGroup"))
                .setObject(group)
                .executeUpdate();
    }

    public void removeGroup(long groupId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.deleteGroup"))
                .setLong("id", groupId)
                .executeUpdate();
    }

    public void linkGroup(long userId, long groupId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.linkGroup"))
                .setLong("userId", userId)
                .setLong("groupId", groupId)
                .executeUpdate();
    }

    public void unlinkGroup(long userId, long groupId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.unlinkGroup"))
                .setLong("userId", userId)
                .setLong("groupId", groupId)
                .executeUpdate();
    }

    public Collection<Position> getPositions(long deviceId, Date from, Date to) throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectPositions"))
                .setLong("deviceId", deviceId)
                .setDate("from", from)
                .setDate("to", to)
                .executeQuery(Position.class);
    }

    public Position getPosition(long positionId) throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectPosition"))
                .setLong("id", positionId)
                .executeQuerySingle(Position.class);
    }

    public void addPosition(Position position) throws SQLException {
        position.setId(QueryBuilder.create(dataSource, getQuery("database.insertPosition"), true)
                .setDate("now", new Date())
                .setObject(position)
                .executeUpdate());
    }

    public void updateLatestPosition(Position position) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.updateLatestPosition"))
                .setDate("now", new Date())
                .setObject(position)
                .executeUpdate();
    }

    public Collection<Position> getLatestPositions() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectLatestPositions"))
                .executeQuery(Position.class);
    }

    public void clearPositionsHistory() throws SQLException {
        long historyDays = config.getInteger("database.positionsHistoryDays");
        if (historyDays != 0) {
            QueryBuilder.create(dataSource, getQuery("database.deletePositions"))
                    .setDate("serverTime", new Date(System.currentTimeMillis() - historyDays * 24 * 3600 * 1000))
                    .executeUpdate();
        }
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

    public Event getEvent(long eventId) throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectEvent"))
                .setLong("id", eventId)
                .executeQuerySingle(Event.class);
    }

    public void addEvent(Event event) throws SQLException {
        event.setId(QueryBuilder.create(dataSource, getQuery("database.insertEvent"), true)
                .setObject(event)
                .executeUpdate());
    }

    public Collection<Event> getEvents(long deviceId, Date from, Date to) throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectEvents"))
                .setLong("deviceId", deviceId)
                .setDate("from", from)
                .setDate("to", to)
                .executeQuery(Event.class);
    }

    public Collection<Geofence> getGeofences() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectGeofencesAll"))
                .executeQuery(Geofence.class);
    }

    public void addGeofence(Geofence geofence) throws SQLException {
        geofence.setId(QueryBuilder.create(dataSource, getQuery("database.insertGeofence"), true)
                .setObject(geofence)
                .executeUpdate());
    }

    public void updateGeofence(Geofence geofence) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.updateGeofence"))
                .setObject(geofence)
                .executeUpdate();
    }

    public void removeGeofence(long geofenceId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.deleteGeofence"))
                .setLong("id", geofenceId)
                .executeUpdate();
    }

    public Collection<GeofencePermission> getGeofencePermissions() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectGeofencePermissions"))
                .executeQuery(GeofencePermission.class);
    }

    public void linkGeofence(long userId, long geofenceId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.linkGeofence"))
                .setLong("userId", userId)
                .setLong("geofenceId", geofenceId)
                .executeUpdate();
    }

    public void unlinkGeofence(long userId, long geofenceId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.unlinkGeofence"))
                .setLong("userId", userId)
                .setLong("geofenceId", geofenceId)
                .executeUpdate();
    }

    public Collection<GroupGeofence> getGroupGeofences() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectGroupGeofences"))
                .executeQuery(GroupGeofence.class);
    }

    public void linkGroupGeofence(long groupId, long geofenceId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.linkGroupGeofence"))
                .setLong("groupId", groupId)
                .setLong("geofenceId", geofenceId)
                .executeUpdate();
    }

    public void unlinkGroupGeofence(long groupId, long geofenceId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.unlinkGroupGeofence"))
                .setLong("groupId", groupId)
                .setLong("geofenceId", geofenceId)
                .executeUpdate();
    }

    public Collection<DeviceGeofence> getDeviceGeofences() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectDeviceGeofences"))
                .executeQuery(DeviceGeofence.class);
    }

    public void linkDeviceGeofence(long deviceId, long geofenceId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.linkDeviceGeofence"))
                .setLong("deviceId", deviceId)
                .setLong("geofenceId", geofenceId)
                .executeUpdate();
    }

    public void unlinkDeviceGeofence(long deviceId, long geofenceId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.unlinkDeviceGeofence"))
                .setLong("deviceId", deviceId)
                .setLong("geofenceId", geofenceId)
                .executeUpdate();
    }

    public Collection<Notification> getNotifications() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectNotifications"))
                .executeQuery(Notification.class);
    }

    public void addNotification(Notification notification) throws SQLException {
        notification.setId(QueryBuilder.create(dataSource, getQuery("database.insertNotification"), true)
                .setObject(notification)
                .executeUpdate());
    }

    public void updateNotification(Notification notification) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.updateNotification"))
                .setObject(notification)
                .executeUpdate();
    }

    public void removeNotification(Notification notification) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.deleteNotification"))
                .setLong("id", notification.getId())
                .executeUpdate();
    }

    public Collection<AttributeAlias> getAttributeAliases() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectAttributeAliases"))
                .executeQuery(AttributeAlias.class);
    }

    public void addAttributeAlias(AttributeAlias attributeAlias) throws SQLException {
        attributeAlias.setId(QueryBuilder.create(dataSource, getQuery("database.insertAttributeAlias"), true)
                .setObject(attributeAlias)
                .executeUpdate());
    }

    public void updateAttributeAlias(AttributeAlias attributeAlias) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.updateAttributeAlias"))
                .setObject(attributeAlias)
                .executeUpdate();
    }

    public void removeAttributeAlias(long attributeAliasId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.deleteAttributeAlias"))
                .setLong("id", attributeAliasId)
                .executeUpdate();
    }

    public Collection<Statistics> getStatistics(Date from, Date to) throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectStatistics"))
                .setDate("from", from)
                .setDate("to", to)
                .executeQuery(Statistics.class);
    }

    public void addStatistics(Statistics statistics) throws SQLException {
        statistics.setId(QueryBuilder.create(dataSource, getQuery("database.insertStatistics"), true)
                .setObject(statistics)
                .executeUpdate());
    }

    public Collection<Calendar> getCalendars() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectCalendarsAll"))
                .executeQuery(Calendar.class);
    }

    public void addCalendar(Calendar calendar) throws SQLException {
        calendar.setId(QueryBuilder.create(dataSource, getQuery("database.insertCalendar"), true)
                .setObject(calendar)
                .executeUpdate());
    }

    public void updateCalendar(Calendar calendar) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.updateCalendar"))
                .setObject(calendar)
                .executeUpdate();
    }

    public void removeCalendar(long calendarId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.deleteCalendar"))
                .setLong("id", calendarId)
                .executeUpdate();
    }

    public Collection<CalendarPermission> getCalendarPermissions() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectCalendarPermissions"))
                .executeQuery(CalendarPermission.class);
    }

    public void linkCalendar(long userId, long calendarId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.linkCalendar"))
                .setLong("userId", userId)
                .setLong("calendarId", calendarId)
                .executeUpdate();
    }

    public void unlinkCalendar(long userId, long calendarId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.unlinkCalendar"))
                .setLong("userId", userId)
                .setLong("calendarId", calendarId)
                .executeUpdate();
    }

    public Collection<UserPermission> getUserPermissions() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectUserPermissions"))
                .executeQuery(UserPermission.class);
    }

    public void linkUser(long userId, long managedUserId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.linkUser"))
                .setLong("userId", userId)
                .setLong("managedUserId", managedUserId)
                .executeUpdate();
    }

    public void unlinkUser(long userId, long managedUserId) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.unlinkUser"))
                .setLong("userId", userId)
                .setLong("managedUserId", managedUserId)
                .executeUpdate();
    }

}
