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
import java.text.SimpleDateFormat;
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
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Permission;
import org.traccar.model.BaseModel;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.model.Statistics;
import org.traccar.model.User;

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

    public void updateDeviceStatus(Device device) throws SQLException {
        QueryBuilder.create(dataSource, getQuery("database.updateDeviceStatus"))
                .setObject(device)
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

    public void clearHistory() throws SQLException {
        long historyDays = config.getInteger("database.historyDays");
        if (historyDays != 0) {
            Date timeLimit = new Date(System.currentTimeMillis() - historyDays * 24 * 3600 * 1000);
            Log.debug("Clearing history earlier than " + new SimpleDateFormat(Log.DATE_FORMAT).format(timeLimit));
            QueryBuilder.create(dataSource, getQuery("database.deletePositions"))
                    .setDate("serverTime", timeLimit)
                    .executeUpdate();
            QueryBuilder.create(dataSource, getQuery("database.deleteEvents"))
                    .setDate("serverTime", timeLimit)
                    .executeUpdate();
        }
    }

    public Server getServer() throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectServers"))
                .executeQuerySingle(Server.class);
    }

    public Event getEvent(long eventId) throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectEvent"))
                .setLong("id", eventId)
                .executeQuerySingle(Event.class);
    }

    public Collection<Event> getEvents(long deviceId, Date from, Date to) throws SQLException {
        return QueryBuilder.create(dataSource, getQuery("database.selectEvents"))
                .setLong("deviceId", deviceId)
                .setDate("from", from)
                .setDate("to", to)
                .executeQuery(Event.class);
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

    public static Class<?> getClassByName(String name) throws ClassNotFoundException {
        return Class.forName("org.traccar.model."
                + name.substring(0, 1).toUpperCase() + name.replace("Id", "").substring(1));
    }

    public static String makeNameId(Class<?> clazz) {
        String name = clazz.getSimpleName();
        return name.substring(0, 1).toLowerCase() + name.substring(1) + (name.indexOf("Id") == -1 ? "Id" : "");
    }

    public void linkObject(Class<?> owner, long ownerId, Class<?> property, long propertyId, boolean link)
            throws SQLException {
        String query = "database." + (link ? "link" : "unlink") + owner.getSimpleName() + property.getSimpleName();
        QueryBuilder queryBuilder = QueryBuilder.create(dataSource, getQuery(query));

        queryBuilder.setLong(makeNameId(owner), ownerId);
        queryBuilder.setLong(makeNameId(property), propertyId);
        queryBuilder.executeUpdate();
    }

    public <T> Collection<T> getObjects(Class<T> clazz) throws SQLException {
        String query = "database.select" + clazz.getSimpleName() + "s";
        return QueryBuilder.create(dataSource, getQuery(query)).executeQuery(clazz);
    }

    public Collection<Permission> getPermissions(Class<? extends BaseModel> owner,
            Class<? extends BaseModel> property) throws SQLException, ClassNotFoundException {
        String query = "database.select" + owner.getSimpleName() + property.getSimpleName() + "s";
        return QueryBuilder.create(dataSource, getQuery(query)).executePermissionsQuery();
    }

    public void addObject(BaseModel entity) throws SQLException {
        String query = "database.insert" + entity.getClass().getSimpleName();
        entity.setId(QueryBuilder.create(dataSource, getQuery(query), true)
                .setObject(entity)
                .executeUpdate());
    }

    public void updateObject(BaseModel entity) throws SQLException {
        String query = "database.update" + entity.getClass().getSimpleName();
        QueryBuilder.create(dataSource, getQuery(query))
                .setObject(entity)
                .executeUpdate();
    }

    public void removeObject(Class<? extends BaseModel> clazz, long entityId) throws SQLException {
        String query = "database.delete" + clazz.getSimpleName();
        QueryBuilder.create(dataSource, getQuery(query))
                .setLong("id", entityId)
                .executeUpdate();
    }

}
