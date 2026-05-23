/*
 * Copyright 2015 - 2026 Anton Tananaev (anton@traccar.org)
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.ReflectionCache;
import org.traccar.model.Permission;

import javax.sql.DataSource;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class QueryBuilder implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryBuilder.class);

    private final Config config;
    private final ObjectMapper objectMapper;

    private final Connection connection;
    private final PreparedStatement statement;
    private final String query;
    private final boolean returnGeneratedKeys;

    private QueryBuilder(
            Config config, DataSource dataSource, ObjectMapper objectMapper,
            String query, boolean returnGeneratedKeys) throws SQLException {
        this.config = config;
        this.objectMapper = objectMapper;
        this.query = query;
        this.returnGeneratedKeys = returnGeneratedKeys;
        connection = dataSource.getConnection();
        try {
            if (returnGeneratedKeys) {
                statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            } else {
                statement = connection.prepareStatement(query);
            }
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
    }

    public static QueryBuilder create(
            Config config, DataSource dataSource, ObjectMapper objectMapper, String query) throws SQLException {
        return new QueryBuilder(config, dataSource, objectMapper, query, false);
    }

    public static QueryBuilder create(
            Config config, DataSource dataSource, ObjectMapper objectMapper, String query,
            boolean returnGeneratedKeys) throws SQLException {
        return new QueryBuilder(config, dataSource, objectMapper, query, returnGeneratedKeys);
    }

    public void setBoolean(int index, boolean value) throws SQLException {
        statement.setBoolean(index + 1, value);
    }

    public void setInteger(int index, int value) throws SQLException {
        statement.setInt(index + 1, value);
    }

    public void setLong(int index, long value) throws SQLException {
        setLong(index, value, false);
    }

    public void setLong(int index, long value, boolean nullIfZero) throws SQLException {
        if (value == 0 && nullIfZero) {
            statement.setNull(index + 1, Types.BIGINT);
        } else {
            statement.setLong(index + 1, value);
        }
    }

    public void setDouble(int index, double value) throws SQLException {
        statement.setDouble(index + 1, value);
    }

    public void setString(int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index + 1, Types.VARCHAR);
        } else {
            statement.setString(index + 1, value);
        }
    }

    public void setDate(int index, Date value) throws SQLException {
        if (value == null) {
            statement.setNull(index + 1, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index + 1, new Timestamp(value.getTime()));
        }
    }

    public void setBlob(int index, byte[] value) throws SQLException {
        if (value == null) {
            statement.setNull(index + 1, Types.BLOB);
        } else {
            statement.setBytes(index + 1, value);
        }
    }

    public void setValue(int index, Object value) throws SQLException {
        switch (value) {
            case Boolean booleanValue -> setBoolean(index, booleanValue);
            case Integer integerValue -> setInteger(index, integerValue);
            case Long longValue -> setLong(index, longValue);
            case Double doubleValue -> setDouble(index, doubleValue);
            case String stringValue -> setString(index, stringValue);
            case Date dateValue -> setDate(index, dateValue);
            default -> {}
        }
    }

    public void setObject(Object object, List<String> columns) throws SQLException {
        try {
            for (int index = 0; index < columns.size(); index++) {
                String column = columns.get(index);
                var property = ReflectionCache.getProperties(object.getClass(), "get").get(column);
                Class<?> returnType = property.type();
                Object value = property.handle().invokeExact(object);
                if (returnType.equals(boolean.class)) {
                    setBoolean(index, (Boolean) value);
                } else if (returnType.equals(int.class)) {
                    setInteger(index, (Integer) value);
                } else if (returnType.equals(long.class)) {
                    setLong(index, (Long) value, column.endsWith("Id"));
                } else if (returnType.equals(double.class)) {
                    setDouble(index, (Double) value);
                } else if (returnType.equals(String.class)) {
                    setString(index, (String) value);
                } else if (returnType.equals(Date.class)) {
                    setDate(index, (Date) value);
                } else if (returnType.equals(byte[].class)) {
                    setBlob(index, (byte[]) value);
                } else {
                    setString(index, objectMapper.writeValueAsString(value));
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("Set object error", e);
        }
    }

    private interface ResultSetProcessor<T> {
        void process(T object, ResultSet resultSet) throws Throwable;
    }

    private <T> void addProcessors(
            List<ResultSetProcessor<T>> processors,
            final Class<?> parameterType, final MethodHandle handle, final int columnIndex) {
        if (parameterType.equals(boolean.class)) {
            processors.add((object, resultSet) -> {
                handle.invokeExact(object, (Object) resultSet.getBoolean(columnIndex));
            });
        } else if (parameterType.equals(int.class)) {
            processors.add((object, resultSet) -> {
                handle.invokeExact(object, (Object) resultSet.getInt(columnIndex));
            });
        } else if (parameterType.equals(long.class)) {
            processors.add((object, resultSet) -> {
                handle.invokeExact(object, (Object) resultSet.getLong(columnIndex));
            });
        } else if (parameterType.equals(double.class)) {
            processors.add((object, resultSet) -> {
                handle.invokeExact(object, (Object) resultSet.getDouble(columnIndex));
            });
        } else if (parameterType.equals(String.class)) {
            processors.add((object, resultSet) -> {
                handle.invokeExact(object, (Object) resultSet.getString(columnIndex));
            });
        } else if (parameterType.equals(Date.class)) {
            processors.add((object, resultSet) -> {
                Timestamp timestamp = resultSet.getTimestamp(columnIndex);
                if (timestamp != null) {
                    handle.invokeExact(object, (Object) new Date(timestamp.getTime()));
                }
            });
        } else if (parameterType.equals(byte[].class)) {
            processors.add((object, resultSet) -> {
                handle.invokeExact(object, (Object) resultSet.getBytes(columnIndex));
            });
        } else {
            processors.add((object, resultSet) -> {
                String value = resultSet.getString(columnIndex);
                if (value != null && !value.isEmpty()) {
                    handle.invokeExact(object, (Object) objectMapper.readValue(value, parameterType));
                }
            });
        }
    }

    private void logQuery() {
        if (config.getBoolean(Keys.LOGGER_QUERIES)) {
            LOGGER.info(query);
        }
    }

    public <T> Stream<T> executeQueryStreamed(Class<T> clazz) throws SQLException {
        ResultSet resultSet = null;
        try {
            logQuery();

            resultSet = statement.executeQuery();
            ResultSetMetaData resultMetaData = resultSet.getMetaData();

            Map<String, Integer> columnIndexes = new HashMap<>();
            for (int i = 1; i <= resultMetaData.getColumnCount(); i++) {
                columnIndexes.put(resultMetaData.getColumnLabel(i).toLowerCase(Locale.ROOT), i);
            }

            List<ResultSetProcessor<T>> processors = new ArrayList<>();
            for (var property : ReflectionCache.getProperties(clazz, "set").values()) {
                Integer columnIndex = columnIndexes.get(property.lowerCaseName());
                if (columnIndex != null) {
                    addProcessors(processors, property.type(), property.handle(), columnIndex);
                }
            }

            final Constructor<T> constructor = ReflectionCache.getConstructor(clazz);

            final ResultSet retainedResultSet = resultSet;
            return StreamSupport.stream(
                    new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED) {
                        @Override
                        public boolean tryAdvance(Consumer<? super T> action) {
                            try {
                                if (retainedResultSet.next()) {
                                    T object = constructor.newInstance();
                                    for (ResultSetProcessor<T> processor : processors) {
                                        try {
                                            processor.process(object, retainedResultSet);
                                        } catch (Throwable error) {
                                            LOGGER.warn("Set property error", error);
                                        }
                                    }
                                    action.accept(object);
                                    return true;
                                } else {
                                    return false;
                                }
                            } catch (SQLException | ReflectiveOperationException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }, false)
                    .onClose(() -> {
                        try {
                            try {
                                retainedResultSet.close();
                            } finally {
                                close();
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (Exception e) {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException ignored) {}
            }
            try {
                close();
            } catch (SQLException ignored) {}
            throw e;
        }
    }

    @Override
    public void close() throws SQLException {
        try {
            statement.close();
        } finally {
            connection.close();
        }
    }

    public long executeUpdate() throws SQLException {
        logQuery();
        statement.execute();
        if (returnGeneratedKeys) {
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }
        return 0;
    }

    public void addBatch() throws SQLException {
        statement.addBatch();
    }

    public List<Long> executeBatch() throws SQLException {
        logQuery();
        statement.executeBatch();
        List<Long> ids = new ArrayList<>();
        if (returnGeneratedKeys) {
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getLong(1));
                }
            }
        }
        return ids;
    }

    public List<Permission> executePermissionsQuery() throws SQLException {
        List<Permission> result = new ArrayList<>();
        logQuery();
        try (ResultSet resultSet = statement.executeQuery()) {
            ResultSetMetaData resultMetaData = resultSet.getMetaData();
            while (resultSet.next()) {
                LinkedHashMap<String, Long> map = new LinkedHashMap<>();
                for (int i = 1; i <= resultMetaData.getColumnCount(); i++) {
                    String label = resultMetaData.getColumnLabel(i);
                    map.put(label, resultSet.getLong(label));
                }
                result.add(new Permission(map));
            }
        }
        return result;
    }

}
