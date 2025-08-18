/*
 * Copyright 2015 - 2025 Anton Tananaev (anton@traccar.org)
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.ReflectionCache;
import org.traccar.model.Permission;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Method;
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("UnusedReturnValue")
public final class QueryBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryBuilder.class);

    private final Config config;
    private final ObjectMapper objectMapper;

    private Connection connection;
    private PreparedStatement statement;
    private final String query;
    private final boolean returnGeneratedKeys;

    private QueryBuilder(
            Config config, DataSource dataSource, ObjectMapper objectMapper,
            String query, boolean returnGeneratedKeys) throws SQLException {
        this.config = config;
        this.objectMapper = objectMapper;
        this.query = query;
        this.returnGeneratedKeys = returnGeneratedKeys;
        if (query != null) {
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

    private interface ValueSetter {
        void invoke() throws SQLException;
    }

    private QueryBuilder setValue(ValueSetter setter) throws SQLException {
        try {
            setter.invoke();
        } catch (SQLException error) {
            statement.close();
            connection.close();
            throw error;
        }
        return this;
    }

    public QueryBuilder setBoolean(int index, boolean value) throws SQLException {
        return setValue(() -> statement.setBoolean(index + 1, value));
    }

    public QueryBuilder setInteger(int index, int value) throws SQLException {
        return setValue(() -> statement.setInt(index + 1, value));
    }

    public QueryBuilder setLong(int index, long value) throws SQLException {
        return setLong(index, value, false);
    }

    public QueryBuilder setLong(int index, long value, boolean nullIfZero) throws SQLException {
        return setValue(() -> {
            if (value == 0 && nullIfZero) {
                statement.setNull(index + 1, Types.BIGINT);
            } else {
                statement.setLong(index + 1, value);
            }
        });
    }

    public QueryBuilder setDouble(int index, double value) throws SQLException {
        return setValue(() -> statement.setDouble(index + 1, value));
    }

    public QueryBuilder setString(int index, String value) throws SQLException {
        return setValue(() -> {
            if (value == null) {
                statement.setNull(index + 1, Types.VARCHAR);
            } else {
                statement.setString(index + 1, value);
            }
        });
    }

    public QueryBuilder setDate(int index, Date value) throws SQLException {
        return setValue(() -> {
            if (value == null) {
                statement.setNull(index + 1, Types.TIMESTAMP);
            } else {
                statement.setTimestamp(index + 1, new Timestamp(value.getTime()));
            }
        });
    }

    public QueryBuilder setBlob(int index, byte[] value) throws SQLException {
        return setValue(() -> {
            if (value == null) {
                statement.setNull(index + 1, Types.BLOB);
            } else {
                statement.setBytes(index + 1, value);
            }
        });
    }

    public QueryBuilder setValue(int index, Object value) throws SQLException {
        if (value instanceof Boolean booleanValue) {
            setBoolean(index, booleanValue);
        } else if (value instanceof Integer integerValue) {
            setInteger(index, integerValue);
        } else if (value instanceof Long longValue) {
            setLong(index, longValue);
        } else if (value instanceof Double doubleValue) {
            setDouble(index, doubleValue);
        } else if (value instanceof String stringValue) {
            setString(index, stringValue);
        } else if (value instanceof Date dateValue) {
            setDate(index, dateValue);
        }
        return this;
    }

    public QueryBuilder setObject(Object object, List<String> columns) throws SQLException {
        try {
            for (int index = 0; index < columns.size(); index++) {
                String column = columns.get(index);
                Method method = ReflectionCache.getProperties(object.getClass(), "get").get(column).method();
                if (method.getReturnType().equals(boolean.class)) {
                    setBoolean(index, (Boolean) method.invoke(object));
                } else if (method.getReturnType().equals(int.class)) {
                    setInteger(index, (Integer) method.invoke(object));
                } else if (method.getReturnType().equals(long.class)) {
                    setLong(index, (Long) method.invoke(object), column.endsWith("Id"));
                } else if (method.getReturnType().equals(double.class)) {
                    setDouble(index, (Double) method.invoke(object));
                } else if (method.getReturnType().equals(String.class)) {
                    setString(index, (String) method.invoke(object));
                } else if (method.getReturnType().equals(Date.class)) {
                    setDate(index, (Date) method.invoke(object));
                } else if (method.getReturnType().equals(byte[].class)) {
                    setBlob(index, (byte[]) method.invoke(object));
                } else {
                    setString(index, objectMapper.writeValueAsString(method.invoke(object)));
                }
            }
        } catch (ReflectiveOperationException | JsonProcessingException e) {
            LOGGER.warn("Set object error", e);
        }

        return this;
    }

    private interface ResultSetProcessor<T> {
        void process(T object, ResultSet resultSet) throws ReflectiveOperationException, IOException, SQLException;
    }

    private <T> void addProcessors(
            List<ResultSetProcessor<T>> processors,
            final Class<?> parameterType, final Method method, final String name) {
        if (parameterType.equals(boolean.class)) {
            processors.add((object, resultSet) -> method.invoke(object, resultSet.getBoolean(name)));
        } else if (parameterType.equals(int.class)) {
            processors.add((object, resultSet) -> method.invoke(object, resultSet.getInt(name)));
        } else if (parameterType.equals(long.class)) {
            processors.add((object, resultSet) -> method.invoke(object, resultSet.getLong(name)));
        } else if (parameterType.equals(double.class)) {
            processors.add((object, resultSet) -> method.invoke(object, resultSet.getDouble(name)));
        } else if (parameterType.equals(String.class)) {
            processors.add((object, resultSet) -> method.invoke(object, resultSet.getString(name)));
        } else if (parameterType.equals(Date.class)) {
            processors.add((object, resultSet) -> {
                Timestamp timestamp = resultSet.getTimestamp(name);
                if (timestamp != null) {
                    method.invoke(object, new Date(timestamp.getTime()));
                }
            });
        } else if (parameterType.equals(byte[].class)) {
            processors.add((object, resultSet) -> method.invoke(object, (Object) resultSet.getBytes(name)));
        } else {
            processors.add((object, resultSet) -> {
                String value = resultSet.getString(name);
                if (value != null && !value.isEmpty()) {
                    method.invoke(object, objectMapper.readValue(value, parameterType));
                }
            });
        }
    }

    private void logQuery() {
        if (config.getBoolean(Keys.LOGGER_QUERIES)) {
            LOGGER.info(query);
        }
    }

    public <T> List<T> executeQuery(Class<T> clazz) throws SQLException {
        try (var stream = executeQueryStreamed(clazz)) {
            return stream.toList();
        }
    }

    public <T> Stream<T> executeQueryStreamed(Class<T> clazz) throws SQLException {
        if (query == null) {
            return Stream.empty();
        }
        ResultSet resultSet = null;
        try {
            logQuery();

            resultSet = statement.executeQuery();
            ResultSetMetaData resultMetaData = resultSet.getMetaData();

            List<ResultSetProcessor<T>> processors = new ArrayList<>();
            for (var entry : ReflectionCache.getProperties(clazz, "set").entrySet()) {
                final String name = entry.getKey();
                boolean column = false;
                for (int i = 1; i <= resultMetaData.getColumnCount(); i++) {
                    if (name.equalsIgnoreCase(resultMetaData.getColumnLabel(i))) {
                        column = true;
                        break;
                    }
                }
                if (column) {
                    Method method = entry.getValue().method();
                    addProcessors(processors, method.getParameterTypes()[0], method, name);
                }
            }

            final ResultSet retainedResultSet = resultSet;
            return StreamSupport.stream(
                    new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED) {
                        @Override
                        public boolean tryAdvance(Consumer<? super T> action) {
                            try {
                                if (retainedResultSet.next()) {
                                    T object = clazz.getDeclaredConstructor().newInstance();
                                    for (ResultSetProcessor<T> processor : processors) {
                                        try {
                                            processor.process(object, retainedResultSet);
                                        } catch (ReflectiveOperationException | IOException error) {
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
                    .onClose(() -> close(retainedResultSet));
        } catch (Exception e) {
            close(resultSet);
            throw e;
        }
    }

    private void close(ResultSet resultSet) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
            statement.close();
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long executeUpdate() throws SQLException {
        if (query != null) {
            try {
                logQuery();
                statement.execute();
                if (returnGeneratedKeys) {
                    try (ResultSet resultSet = statement.getGeneratedKeys()) {
                        if (resultSet.next()) {
                            return resultSet.getLong(1);
                        }
                    }
                }
            } finally {
                statement.close();
                connection.close();
            }
        }
        return 0;
    }

    public List<Permission> executePermissionsQuery() throws SQLException {
        List<Permission> result = new LinkedList<>();
        if (query != null) {
            try {
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
            } finally {
                statement.close();
                connection.close();
            }
        }

        return result;
    }

}
