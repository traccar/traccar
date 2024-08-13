/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.exception.LockException;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import jakarta.inject.Singleton;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;

public class DatabaseModule extends AbstractModule {

    @Singleton
    @Provides
    public static DataSource provideDataSource(
            Config config) throws ReflectiveOperationException, IOException, LiquibaseException {

        String driverFile = config.getString(Keys.DATABASE_DRIVER_FILE);
        if (driverFile != null) {
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            try {
                Method method = classLoader.getClass().getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(classLoader, new File(driverFile).toURI().toURL());
            } catch (NoSuchMethodException e) {
                Method method = classLoader.getClass()
                        .getDeclaredMethod("appendToClassPathForInstrumentation", String.class);
                method.setAccessible(true);
                method.invoke(classLoader, driverFile);
            }
        }

        String driver = config.getString(Keys.DATABASE_DRIVER);
        if (driver != null) {
            Class.forName(driver);
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(driver);
        hikariConfig.setJdbcUrl(config.getString(Keys.DATABASE_URL));
        hikariConfig.setUsername(config.getString(Keys.DATABASE_USER));
        hikariConfig.setPassword(config.getString(Keys.DATABASE_PASSWORD));
        hikariConfig.setConnectionInitSql(config.getString(Keys.DATABASE_CHECK_CONNECTION));
        hikariConfig.setIdleTimeout(600000);

        int maxPoolSize = config.getInteger(Keys.DATABASE_MAX_POOL_SIZE);
        if (maxPoolSize != 0) {
            hikariConfig.setMaximumPoolSize(maxPoolSize);
        }

        DataSource dataSource = new HikariDataSource(hikariConfig);

        String changelog = config.getString(Keys.DATABASE_CHANGELOG);
        if (changelog != null && !changelog.isEmpty()) {

            ResourceAccessor resourceAccessor = new DirectoryResourceAccessor(new File("."));

            System.setProperty("liquibase.changelogLockWaitTimeInMinutes", "1");

            try {
                Database database = DatabaseFactory.getInstance().openDatabase(
                        config.getString(Keys.DATABASE_URL),
                        config.getString(Keys.DATABASE_USER),
                        config.getString(Keys.DATABASE_PASSWORD),
                        config.getString(Keys.DATABASE_DRIVER),
                        null, null, null, resourceAccessor);

                try (Liquibase liquibase = new Liquibase(changelog, resourceAccessor, database)) {
                    liquibase.clearCheckSums();
                    liquibase.update(new Contexts());
                }
            } catch (LockException e) {
                throw new DatabaseLockException();
            }
        }

        return dataSource;
    }

}

class DatabaseLockException extends RuntimeException {
    DatabaseLockException() {
        super("Database is in a locked state. "
                + "It could be due to early service termination on a previous launch. "
                + "To unlock you can run this query: 'UPDATE DATABASECHANGELOGLOCK SET locked = 0'. "
                + "Make sure the schema is up to date before unlocking the database.");
    }
}
